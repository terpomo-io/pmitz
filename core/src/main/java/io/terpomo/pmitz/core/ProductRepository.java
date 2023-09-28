package io.terpomo.pmitz.core;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    List<String> getProductIds ();

    Optional<Product> getProductById (String productId);

    void addProduct (Product product);

    void updateProduct (Product product);
    
    //TODO remove product
    void removeProduct (Product product);

}
