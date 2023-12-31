package com.sharetreats.chatbot.module.service;

import com.sharetreats.chatbot.module.dto.ViberRichMediaMessage;
import com.sharetreats.chatbot.module.entity.Brand;
import com.sharetreats.chatbot.module.entity.Product;
import com.sharetreats.chatbot.module.repository.BrandRepository;
import com.sharetreats.chatbot.module.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.sharetreats.chatbot.module.dto.ViberRichMediaMessage.RichMedia;


@Service
@RequiredArgsConstructor
public class RichMediaService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    public ResponseEntity<ViberRichMediaMessage> sendProductsByBrand(String receiverId, Long brandId, String auth_token) {
        Brand brand = brandRepository.findById(brandId).orElse(null);
        if (brand == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String brandName = brand.getName();
        List<Product> products = productRepository.findByBrandName(brandName);

        String sendUrl = "https://chatapi.viber.com/pa/send_message";

        ViberRichMediaMessage richMediaMessage = convertToRichMediaMessages(products);
        return sendRichMediaMessage(receiverId, auth_token, sendUrl, richMediaMessage);

    }

    public ResponseEntity<ViberRichMediaMessage> sendProductDetail(String receiverId, Long productId, String authToken) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        String sendUrl = "https://chatapi.viber.com/pa/send_message";
        ViberRichMediaMessage richMediaMessage = convertToRichMediaMessages(product);
        return sendRichMediaMessage(receiverId, authToken, sendUrl, richMediaMessage);
    }

    private ResponseEntity<ViberRichMediaMessage> sendRichMediaMessage(String receiverId, String authToken, String sendUrl, ViberRichMediaMessage richMediaMessage) {
        richMediaMessage.setReceiver(receiverId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Viber-Auth-Token", authToken);

        HttpEntity<ViberRichMediaMessage> httpEntity = new HttpEntity<>(richMediaMessage, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(sendUrl, httpEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok(richMediaMessage);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ViberRichMediaMessage convertToRichMediaMessages(List<Product> products) {
        List<Map<String ,Object>> buttons = new ArrayList<>();
        for (Product product : products) {
            Map<String, Object> image = new LinkedHashMap<>();
            makeProductImage(buttons, product, image);

            Map<String, Object> productText = new LinkedHashMap<>();
            makeProductText(buttons, product, productText);

            Map<String, Object> buyButton = new LinkedHashMap<>();
            makeSendTreatsButton(buttons, product, buyButton);

            Map<String, Object> detailButton = new LinkedHashMap<>();
            makeDetailButton(buttons, product, detailButton);
        }

        RichMedia richMedia = RichMedia.builder()
                .type("rich_media")
                .buttonsGroupColumns(6)
                .buttonsGroupRows(7)
                .bgColor("#FFFFFF")
                .buttons(buttons)
                .build();

        return ViberRichMediaMessage.builder()
                .minApiVersion(7)
                .type("rich_media")
                .richMedia(richMedia)
                .build();
    }

    public ViberRichMediaMessage convertToRichMediaMessages(Product product) {
        List<Map<String, Object>> buttons = new ArrayList<>();

        Map<String, Object> productDetailInfo = new LinkedHashMap<>();
        makeProductDetailInfo(buttons, product, productDetailInfo);

        Map<String, Object> sendTreatsButton = new LinkedHashMap<>();
        makeSendTreatsButton(buttons, product, sendTreatsButton);

        Map<String, Object> sendDiscountShopButton = new LinkedHashMap<>();
        makeDiscountPlaceButton(buttons, product, sendDiscountShopButton);


        RichMedia richMedia = RichMedia.builder()
                .type("rich_media")
                .buttonsGroupColumns(6)
                .buttonsGroupRows(7)
                .bgColor("#FFFFFF")
                .buttons(buttons)
                .build();

        return ViberRichMediaMessage.builder()
                .minApiVersion(7)
                .type("rich_media")
                .richMedia(richMedia)
                .build();
    }


    private void makeProductImage(List<Map<String, Object>> buttons, Product product, Map<String, Object> image) {
        image.put("Columns", 6);
        image.put("Rows", 3);
        image.put("ActionType", "open-url");
        image.put("ActionBody", "https://www.google.com");
        image.put("Image", product.getImage());

        buttons.add(image);
    }

    private void makeProductText(List<Map<String, Object>> buttons, Product product, Map<String, Object> productText) {
        productText.put("Columns", 6);
        productText.put("Rows", 2);
        productText.put("Text", "<font color=#323232><b>" + product.getBrandName() + "</b></font><font color=#777777><br>" + product.getName() + " </font><font color=#6fc133>" + product.getDiscountPrice() + "</font>");
        productText.put("ActionType", "open-url");
        productText.put("ActionBody", "https://www.google.com");
        productText.put("TextSize", "medium");
        productText.put("TextVAlign", "middle");
        productText.put("TextHAlign", "left");

        buttons.add(productText);
    }

    private void makeSendTreatsButton(List<Map<String, Object>>buttons, Product product, Map<String, Object> buyButton) {
        buyButton.put("Columns", 6);
        buyButton.put("Rows", 1);
        buyButton.put("ActionType", "reply");
        buyButton.put("ActionBody", "send treats " + product.getId());
        buyButton.put("Text", "<font color=#ffffff>Send Treats</font>");
        buyButton.put("TextSize", "medium");
        buyButton.put("TextVAlign", "middle");
        buyButton.put("TextHAlign", "middle");

        buttons.add(buyButton);
    }

    private void makeDetailButton(List<Map<String, Object>>buttons, Product product, Map<String, Object> detailButton) {
        detailButton.put("Columns", 6);
        detailButton.put("Rows", 1);
        detailButton.put("ActionType", "reply");
        detailButton.put("ActionBody", "view more " + product.getId());
        detailButton.put("Text", "<font color=#ffffff>view more</font>");
        detailButton.put("TextSize", "medium");
        detailButton.put("TextVAlign", "middle");
        detailButton.put("TextHAlign", "middle");

        buttons.add(detailButton);
    }

    private void makeProductDetailInfo(List<Map<String, Object>> buttons, Product product, Map<String, Object> productText) {
        String text = String.format(
                "브랜드명: %s<br>상품명: %s<br>상품 가격: %d<br>할인 가격: %d<br>상품 설명<br> %s",
                product.getBrandName(),
                product.getName(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getDescription()
        );

        productText.put("Columns", 6);
        productText.put("Rows", 4);
        productText.put("Text", text);
        productText.put("ActionType", "open-url");
        productText.put("ActionBody", product.getDiscountShop());
        productText.put("TextSize", "medium");
        productText.put("TextVAlign", "middle");
        productText.put("TextHAlign", "left");

        buttons.add(productText);
    }

    private void makeDiscountPlaceButton(List<Map<String, Object>> buttons, Product product, Map<String, Object> discountShopButton) {
        discountShopButton.put("Columns", 6);
        discountShopButton.put("Rows", 1);
        discountShopButton.put("ActionType", "open-url");
        discountShopButton.put("ActionBody", product.getDiscountShop());
        discountShopButton.put("Text", "<font color=#ffffff>Discount Place</font>");
        discountShopButton.put("TextSize", "medium");
        discountShopButton.put("TextVAlign", "middle");
        discountShopButton.put("TextHAlign", "middle");

        buttons.add(discountShopButton);
    }
}
