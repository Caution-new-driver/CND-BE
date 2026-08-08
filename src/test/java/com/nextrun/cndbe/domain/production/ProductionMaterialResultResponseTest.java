package com.nextrun.cndbe.domain.production;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.production.dto.ProductionMaterialResultResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductionMaterialResultResponseTest {

    @Test
    void 소재_ID를_UUID_타입으로_반환한다() {
        UUID materialId = UUID.randomUUID();
        Material material = Material.builder()
                .id(materialId)
                .materialCode("TEST-001")
                .build();
        ProductionMaterialResult result = ProductionMaterialResult.builder()
                .material(material)
                .materialRole(MaterialRole.MAIN)
                .supportedMiniBagQuantity(1)
                .luggageTagQuantity(0)
                .availableAreaMm2(100d)
                .usedAreaMm2(60d)
                .remainingAreaMm2(40d)
                .build();

        ProductionMaterialResultResponse response =
                ProductionMaterialResultResponse.from(result, List.of());

        assertEquals(materialId, response.materialId());
    }
}
