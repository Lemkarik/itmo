public class Checker {
    public static boolean hit(float x, float y, float r) {
        return inRect(x, y, r) || inTriangle(x, y, r) || inCircle(x, y, r);
    }

    private static boolean inRect(float x, float y, float r) {
        return x >= 0 && y <= 0 && x <= r && y >= -r;
    }

    private static boolean inTriangle(float x, float y, float r) {
        return x <= 0 && y >= 0 && y <= x + r;
    }

    private static boolean inCircle(float x, float y, float r) {
        return x >= 0 && y >= 0 && x * x + y * y <= r * r;
    }
}
