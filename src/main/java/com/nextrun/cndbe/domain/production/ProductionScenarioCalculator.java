package com.nextrun.cndbe.domain.production;

import com.nextrun.cndbe.common.calculation.PatternPiece;
import com.nextrun.cndbe.common.calculation.PatternPlacementCalculator;
import com.nextrun.cndbe.common.calculation.RemainingRegion;
import com.nextrun.cndbe.common.calculation.SheetPlacementResult;
import com.nextrun.cndbe.common.calculation.TemplatePatternParser;
import com.nextrun.cndbe.domain.matching.DropMaterialSelection;
import com.nextrun.cndbe.domain.material.Material;
import com.nextrun.cndbe.domain.material.Template;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

// b12의 수치 계산 담당. AI를 사용하지 않고 템플릿 치수와 선택 소재 크기로 항상 같은 결과를 만든다.
// 제작 수량은 PatternPlacementCalculator가 실제 배치 가능하다고 확인한 보수적 수량이다.
@Component
public class ProductionScenarioCalculator {

    private final TemplatePatternParser templatePatternParser;
    private final PatternPlacementCalculator placementCalculator;

    public ProductionScenarioCalculator(
            TemplatePatternParser templatePatternParser,
            PatternPlacementCalculator placementCalculator
    ) {
        this.templatePatternParser = templatePatternParser;
        this.placementCalculator = placementCalculator;
    }

    public ProductionCalculationResult calculate(
            DropMaterialSelection selection,
            Template miniBagTemplate,
            Template luggageTagTemplate
    ) {
        if (selection == null || selection.getMainMaterial() == null) {
            throw new IllegalStateException(
                    "제작 가능성을 계산하려면 주 소재를 먼저 선택해야 합니다."
            );
        }

        List<PatternPiece> miniBagPieces = templatePatternParser.parse(
                miniBagTemplate
        );
        List<PatternPiece> tagPieces = templatePatternParser.parse(
                luggageTagTemplate
        );
        if (tagPieces.size() != 1 || tagPieces.getFirst().quantity() != 1) {
            throw new IllegalStateException(
                    "러기지 태그 템플릿은 패턴 조각 1개여야 합니다."
            );
        }

        List<MaterialPlan> plans = buildMaterialPlans(
                selection,
                miniBagPieces
        );
        int miniBagQuantity = plans.stream()
                .mapToInt(MaterialPlan::supportedMiniBagQuantity)
                .min()
                .orElse(0);
        if (miniBagQuantity < 1) {
            throw new IllegalStateException(
                    "선택한 소재로 미니백을 1개 이상 제작할 수 없습니다."
            );
        }

        List<PlacedMaterial> placedMaterials = plans.stream()
                .map(plan -> placeMiniBags(plan, miniBagQuantity))
                .toList();

        ProductionCalculationResult.ScenarioCalculation mainOnly =
                buildMainOnlyScenario(placedMaterials);
        ProductionCalculationResult.ScenarioCalculation withTags =
                buildTagScenario(placedMaterials, tagPieces.getFirst());

        return new ProductionCalculationResult(
                miniBagQuantity,
                List.of(mainOnly, withTags)
        );
    }

    private List<MaterialPlan> buildMaterialPlans(
            DropMaterialSelection selection,
            List<PatternPiece> miniBagPieces
    ) {
        Material main = selection.getMainMaterial();
        validateMaterial(main, "주 소재");

        if (selection.getPointMaterial() == null) {
            return List.of(plan(main, MaterialRole.MAIN, miniBagPieces));
        }

        Material point = selection.getPointMaterial();
        validateMaterial(point, "포인트 소재");

        // 합의한 MVP 규칙: 앞판·뒷판은 주 소재, 옆판/바닥은 포인트 소재가 담당한다.
        List<PatternPiece> pointPieces = miniBagPieces.stream()
                .filter(this::isPointPiece)
                .toList();
        List<PatternPiece> mainPieces = miniBagPieces.stream()
                .filter(piece -> !isPointPiece(piece))
                .toList();
        if (mainPieces.isEmpty() || pointPieces.isEmpty()) {
            throw new IllegalStateException(
                    "포인트 소재에 배정할 옆판/바닥 패턴을 찾을 수 없습니다."
            );
        }

        return List.of(
                plan(main, MaterialRole.MAIN, mainPieces),
                plan(point, MaterialRole.POINT, pointPieces)
        );
    }

    private MaterialPlan plan(
            Material material,
            MaterialRole role,
            List<PatternPiece> pieces
    ) {
        int capacityPerSheet = placementCalculator.calculateCapacityPerSheet(
                material.getWidthMm(),
                material.getHeightMm(),
                pieces
        );
        return new MaterialPlan(
                material,
                role,
                pieces,
                capacityPerSheet * material.getQuantity()
        );
    }

    private PlacedMaterial placeMiniBags(
            MaterialPlan plan,
            int miniBagQuantity
    ) {
        Material material = plan.material();
        SheetPlacementResult result = placementCalculator.placeAcrossSheets(
                material.getWidthMm(),
                material.getHeightMm(),
                material.getQuantity(),
                plan.pieces(),
                miniBagQuantity
        );
        double availableArea = material.getWidthMm()
                * material.getHeightMm()
                * material.getQuantity();
        double bagUsedArea = plan.pieces().stream()
                .mapToDouble(PatternPiece::areaMm2)
                .sum() * miniBagQuantity;

        return new PlacedMaterial(
                plan,
                availableArea,
                bagUsedArea,
                result.remainingRegions()
        );
    }

    private ProductionCalculationResult.ScenarioCalculation
            buildMainOnlyScenario(List<PlacedMaterial> placedMaterials) {
        List<ProductionCalculationResult.MaterialCalculation> materials =
                placedMaterials.stream()
                        .map(placed -> materialCalculation(
                                placed,
                                0,
                                placed.bagUsedAreaMm2(),
                                placed.remainingAfterBags()
                        ))
                        .toList();
        return scenario(
                ScenarioType.MAIN_ONLY,
                0,
                materials
        );
    }

    private ProductionCalculationResult.ScenarioCalculation buildTagScenario(
            List<PlacedMaterial> placedMaterials,
            PatternPiece tagPiece
    ) {
        List<ProductionCalculationResult.MaterialCalculation> materials =
                new ArrayList<>();
        int totalTags = 0;

        // 주 소재와 포인트 소재 양쪽의 남은 영역을 각각 활용하고 태그 수량을 합산한다.
        for (PlacedMaterial placed : placedMaterials) {
            PatternPlacementCalculator.TagPlacementResult tagResult =
                    placementCalculator.fillWithTags(
                            placed.remainingAfterBags(),
                            tagPiece
                    );
            int tags = tagResult.quantity();
            totalTags += tags;
            double usedArea = placed.bagUsedAreaMm2()
                    + tags * tagPiece.areaMm2();
            materials.add(materialCalculation(
                    placed,
                    tags,
                    usedArea,
                    tagResult.remainingRegions()
            ));
        }

        return scenario(
                ScenarioType.WITH_LUGGAGE_TAG,
                totalTags,
                List.copyOf(materials)
        );
    }

    private ProductionCalculationResult.MaterialCalculation
            materialCalculation(
                    PlacedMaterial placed,
                    int tagQuantity,
                    double usedArea,
                    List<RemainingRegion> remainingRegions
            ) {
        double remainingArea = remainingRegions.stream()
                .mapToDouble(RemainingRegion::areaMm2)
                .sum();
        return new ProductionCalculationResult.MaterialCalculation(
                placed.plan().material(),
                placed.plan().role(),
                placed.plan().supportedMiniBagQuantity(),
                tagQuantity,
                placed.availableAreaMm2(),
                usedArea,
                remainingArea,
                remainingRegions
        );
    }

    private ProductionCalculationResult.ScenarioCalculation scenario(
            ScenarioType type,
            int luggageTagQuantity,
            List<ProductionCalculationResult.MaterialCalculation> materials
    ) {
        double totalArea = materials.stream()
                .mapToDouble(ProductionCalculationResult.MaterialCalculation::availableAreaMm2)
                .sum();
        double usedArea = materials.stream()
                .mapToDouble(ProductionCalculationResult.MaterialCalculation::usedAreaMm2)
                .sum();
        double remainingArea = materials.stream()
                .mapToDouble(ProductionCalculationResult.MaterialCalculation::remainingAreaMm2)
                .sum();
        float utilizationRate = totalArea == 0
                ? 0
                : (float) (usedArea / totalArea * 100.0);

        return new ProductionCalculationResult.ScenarioCalculation(
                type,
                luggageTagQuantity,
                totalArea,
                usedArea,
                remainingArea,
                utilizationRate,
                materials
        );
    }

    private boolean isPointPiece(PatternPiece piece) {
        String name = piece.pieceName() == null
                ? ""
                : piece.pieceName().toLowerCase(Locale.ROOT);
        return name.contains("옆판")
                || name.contains("바닥")
                || name.contains("side")
                || name.contains("bottom");
    }

    private void validateMaterial(Material material, String role) {
        if (material.getWidthMm() == null
                || material.getHeightMm() == null
                || material.getQuantity() == null
                || material.getWidthMm() <= 0
                || material.getHeightMm() <= 0
                || material.getQuantity() <= 0) {
            throw new IllegalStateException(
                    role + "의 크기 또는 수량 정보가 올바르지 않습니다."
            );
        }
    }

    private record MaterialPlan(
            Material material,
            MaterialRole role,
            List<PatternPiece> pieces,
            int supportedMiniBagQuantity
    ) {
    }

    private record PlacedMaterial(
            MaterialPlan plan,
            double availableAreaMm2,
            double bagUsedAreaMm2,
            List<RemainingRegion> remainingAfterBags
    ) {
    }
}
