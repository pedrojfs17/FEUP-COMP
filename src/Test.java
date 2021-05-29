public class Test {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println("ola");
        }
    }

    public int test(int N, int[] A) {
        int sum = 0;
        int i = 0;
        while (i < N) {
            int t1 = A[i];
            sum = sum + t1;
            i++;
        }
        return sum;
    }
}
