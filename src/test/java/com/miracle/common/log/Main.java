package com.miracle.common.log;

import com.google.common.collect.Maps;
import com.miracle.common.log.logger.MiracleLogger;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Main {
    public static class ProductModel
    {
        private String model;
        private float price;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public float getPrice() {
            return price;
        }

        public void setPrice(float price) {
            this.price = price;
        }
    }
    public static void main(String[] args) throws InterruptedException {
        log.info("hello {}", "wqj");
        log.error("test-nothrow");
        log.error("test", new RuntimeException("mock runtimeexception"));

        MiracleLogger kklog = MiracleLogger.getLogger(Main.class);

        kklog.business("product",
                "productSeries={},productName={},tags={},desc_model={},desc_price={}",
                "aaa", "bbb", new String[]{"aa", "bb"}, "model", 20.0f);

        ProductModel model1 = new ProductModel();
        model1.setModel("xmodel");
        model1.setPrice(30.0f);
        kklog.business("product",new String[]{"productSeries", "productName", "tags", "desc"},
                new Object[]{"ccc", "ddd", new String[]{"aa", "bb"}, model1});

        Map<String, Object> modelAttrs = Maps.newHashMap();
        modelAttrs.put("model", "ymodel");
        modelAttrs.put("price", 100.0f);
        kklog.business("product",new String[]{"productSeries", "productName", "tags", "desc"},
                new Object[]{"ccc", "ddd", new String[]{"aa", "bb"}, modelAttrs});

        kklog.info("done");
        Thread.sleep(2000);
    }
}
