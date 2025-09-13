public class Validator {
    public static boolean validateX(float x) {
        return -3. < x && x < 3.;
    }

    public static boolean validateY(int y) {
        return y == -3 || y == -2 || y == -1 || y == 0 || y == 1 || y == 2 || y == 3 || y == 4 || y == 5;
    }

    public static boolean validateR(float r) {
        return 2. < r && r < 5.;
    }
}
