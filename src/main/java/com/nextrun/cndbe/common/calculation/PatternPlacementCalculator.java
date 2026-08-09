package com.nextrun.cndbe.common.calculation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

// 직사각형 패턴을 큰 조각부터 배치하는 단순 2차원 재단 계산기.
// 각 소재 장은 물리적으로 분리되어 있으므로 서로 붙이지 않으며, 패턴은 90도 회전을 허용한다.
@Component
public class PatternPlacementCalculator {

    private static final double EPSILON = 0.0001;

    public int calculateCapacityPerSheet(
            double sheetWidthMm,
            double sheetHeightMm,
            List<PatternPiece> pieces
    ) {
        validateSheet(sheetWidthMm, sheetHeightMm);
        validatePieces(pieces);

        double productArea = pieces.stream()
                .mapToDouble(PatternPiece::areaMm2)
                .sum();
        int areaUpperBound = (int) Math.floor(
                sheetWidthMm * sheetHeightMm / productArea
        );

        // 면적으로 가능한 최대치부터 내려오며 실제 2차원 배치가 되는 첫 값을 찾는다.
        for (int count = areaUpperBound; count >= 1; count--) {
            if (tryPlace(
                    List.of(new RemainingRegion(sheetWidthMm, sheetHeightMm)),
                    expandPieces(pieces, count)
            ) != null) {
                return count;
            }
        }
        return 0;
    }

    public SheetPlacementResult placeAcrossSheets(
            double sheetWidthMm,
            double sheetHeightMm,
            int sheetQuantity,
            List<PatternPiece> pieces,
            int productQuantity
    ) {
        validateSheet(sheetWidthMm, sheetHeightMm);
        validatePieces(pieces);
        if (sheetQuantity <= 0 || productQuantity < 0) {
            throw new IllegalArgumentException(
                    "소재 장수와 제품 수량은 올바른 값이어야 합니다."
            );
        }

        int capacityPerSheet = calculateCapacityPerSheet(
                sheetWidthMm,
                sheetHeightMm,
                pieces
        );
        int totalCapacity = capacityPerSheet * sheetQuantity;
        if (productQuantity > totalCapacity) {
            throw new IllegalArgumentException(
                    "선택한 소재에 요청한 수량의 패턴을 배치할 수 없습니다."
            );
        }

        List<RemainingRegion> allRemaining = new ArrayList<>();
        int productsLeft = productQuantity;

        for (int sheet = 0; sheet < sheetQuantity; sheet++) {
            int productsOnSheet = Math.min(capacityPerSheet, productsLeft);
            List<RemainingRegion> initial = List.of(
                    new RemainingRegion(sheetWidthMm, sheetHeightMm)
            );
            List<RemainingRegion> remaining = productsOnSheet == 0
                    ? initial
                    : tryPlace(initial, expandPieces(pieces, productsOnSheet));
            if (remaining == null) {
                throw new IllegalStateException(
                        "계산된 제작 수량을 소재에 배치할 수 없습니다."
                );
            }
            allRemaining.addAll(remaining);
            productsLeft -= productsOnSheet;
        }

        return new SheetPlacementResult(totalCapacity, List.copyOf(allRemaining));
    }

    public TagPlacementResult fillWithTags(
            List<RemainingRegion> initialRegions,
            PatternPiece tagPiece
    ) {
        validatePieces(List.of(tagPiece));
        if (tagPiece.quantity() != 1) {
            throw new IllegalArgumentException(
                    "러기지 태그 템플릿은 패턴 조각 1개여야 합니다."
            );
        }

        List<RemainingRegion> freeRegions = new ArrayList<>(initialRegions);
        int tagQuantity = 0;
        while (true) {
            Placement placement = findBestPlacement(
                    freeRegions,
                    tagPiece.widthMm(),
                    tagPiece.heightMm()
            );
            if (placement == null) {
                break;
            }
            splitPlacedRegion(freeRegions, placement);
            tagQuantity++;
        }
        return new TagPlacementResult(tagQuantity, List.copyOf(freeRegions));
    }

    private List<RemainingRegion> tryPlace(
            List<RemainingRegion> initialRegions,
            List<PieceRectangle> rectangles
    ) {
        List<RemainingRegion> freeRegions = new ArrayList<>(initialRegions);
        for (PieceRectangle rectangle : rectangles) {
            Placement placement = findBestPlacement(
                    freeRegions,
                    rectangle.widthMm(),
                    rectangle.heightMm()
            );
            if (placement == null) {
                return null;
            }
            splitPlacedRegion(freeRegions, placement);
        }
        return freeRegions;
    }

    private List<PieceRectangle> expandPieces(
            List<PatternPiece> pieces,
            int productQuantity
    ) {
        List<PieceRectangle> rectangles = new ArrayList<>();
        for (PatternPiece piece : pieces) {
            int count = piece.quantity() * productQuantity;
            for (int index = 0; index < count; index++) {
                rectangles.add(new PieceRectangle(
                        piece.widthMm(),
                        piece.heightMm()
                ));
            }
        }

        // 긴 조각을 먼저 놓아야 400x60 옆판/바닥 같은 띠 형태가 작은 조각에 가로막히지 않는다.
        rectangles.sort(
                Comparator.comparingDouble(PieceRectangle::longSideMm)
                        .reversed()
                        .thenComparing(
                                Comparator.comparingDouble(
                                        PieceRectangle::areaMm2
                                ).reversed()
                        )
        );
        return rectangles;
    }

    private Placement findBestPlacement(
            List<RemainingRegion> freeRegions,
            double originalWidth,
            double originalHeight
    ) {
        Placement best = null;
        for (int index = 0; index < freeRegions.size(); index++) {
            RemainingRegion free = freeRegions.get(index);
            best = betterOf(
                    best,
                    candidate(index, free, originalWidth, originalHeight)
            );
            if (Math.abs(originalWidth - originalHeight) > EPSILON) {
                best = betterOf(
                        best,
                        candidate(index, free, originalHeight, originalWidth)
                );
            }
        }
        return best;
    }

    private Placement candidate(
            int regionIndex,
            RemainingRegion free,
            double width,
            double height
    ) {
        if (width > free.widthMm() + EPSILON
                || height > free.heightMm() + EPSILON) {
            return null;
        }
        double remainingWidth = free.widthMm() - width;
        double remainingHeight = free.heightMm() - height;
        return new Placement(
                regionIndex,
                free,
                width,
                height,
                Math.min(remainingWidth, remainingHeight),
                Math.max(remainingWidth, remainingHeight)
        );
    }

    private Placement betterOf(Placement current, Placement candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null
                || candidate.shortSideRemainder() < current.shortSideRemainder()
                || (Math.abs(candidate.shortSideRemainder()
                        - current.shortSideRemainder()) < EPSILON
                        && candidate.longSideRemainder()
                        < current.longSideRemainder())) {
            return candidate;
        }
        return current;
    }

    private void splitPlacedRegion(
            List<RemainingRegion> freeRegions,
            Placement placement
    ) {
        freeRegions.remove(placement.regionIndex());
        RemainingRegion free = placement.freeRegion();
        double rightWidth = free.widthMm() - placement.widthMm();
        double bottomHeight = free.heightMm() - placement.heightMm();

        // 남는 쪽이 긴 축을 유지하도록 절단 방향을 선택한다.
        if (rightWidth > bottomHeight) {
            addIfPositive(
                    freeRegions,
                    rightWidth,
                    free.heightMm()
            );
            addIfPositive(
                    freeRegions,
                    placement.widthMm(),
                    bottomHeight
            );
        } else {
            addIfPositive(
                    freeRegions,
                    rightWidth,
                    placement.heightMm()
            );
            addIfPositive(
                    freeRegions,
                    free.widthMm(),
                    bottomHeight
            );
        }
    }

    private void addIfPositive(
            List<RemainingRegion> regions,
            double width,
            double height
    ) {
        if (width > EPSILON && height > EPSILON) {
            regions.add(new RemainingRegion(width, height));
        }
    }

    private void validateSheet(double width, double height) {
        if (!Double.isFinite(width) || !Double.isFinite(height)
                || width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "소재의 가로·세로 길이는 0보다 커야 합니다."
            );
        }
    }

    private void validatePieces(List<PatternPiece> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            throw new IllegalArgumentException(
                    "템플릿 패턴 조각 정보가 없습니다."
            );
        }
        for (PatternPiece piece : pieces) {
            if (piece == null
                    || !Double.isFinite(piece.widthMm())
                    || !Double.isFinite(piece.heightMm())
                    || piece.widthMm() <= 0
                    || piece.heightMm() <= 0
                    || piece.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "템플릿 패턴 조각 정보가 올바르지 않습니다."
                );
            }
        }
    }

    private record PieceRectangle(double widthMm, double heightMm) {
        private double areaMm2() {
            return widthMm * heightMm;
        }

        private double longSideMm() {
            return Math.max(widthMm, heightMm);
        }
    }

    private record Placement(
            int regionIndex,
            RemainingRegion freeRegion,
            double widthMm,
            double heightMm,
            double shortSideRemainder,
            double longSideRemainder
    ) {
    }

    public record TagPlacementResult(
            int quantity,
            List<RemainingRegion> remainingRegions
    ) {
    }
}
