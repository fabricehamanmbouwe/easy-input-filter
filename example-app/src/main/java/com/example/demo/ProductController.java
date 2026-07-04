package com.example.demo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Try it with Postman:
 * <p>
 * POST http://localhost:8080/products
 * <pre>{@code
 * {
 *   "title": "Canape en cuir",
 *   "description": "Tres bon etat, contactez-moi au 06 12 34 56 78",
 *   "internalNote": "  <b>VIP</b>   client    fidele  "
 * }
 * }</pre>
 * → 400 Bad Request, RFC 7807 body, field "description", detector "phone".
 * <p>
 * Try again with a clean description and you will get a 200 with
 * internalNote sanitized to "VIP client fidele".
 */
@RestController
public class ProductController {

    @PostMapping("/products")
    public ProductRequest createProduct(@RequestBody ProductRequest request) {
        // If we reach this line, the request body has already passed
        // through easy-input-filter with zero code written here.
        return request;
    }
}
