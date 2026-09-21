public class Task2 {
    public static void main(String[] args){
        print(5);
        System.out.println(fibonacci1(8));
        System.out.println(fibonacci2(8));
        hanoi(3);
    }



    //1. 判断传入年份是否为闰年
    boolean isLeapYear(int year) {
        if( (year%4==0&&year%100!=0)||(year%400==0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    //2. 例如 n = 5 时，输出：
    //   *
    //  * *         i=1
    // *   *        i=2
    //  * *         i=3
    //   *
    public static void print(int n) {
        int half=(n-1)/2;
        for (int i=0;i<n;i++){
            for(int j=0;j<Math.abs(half-i);j++){
                System.out.print(' ');
            }
            if(i==0||i==n-1){
                System.out.print('*');
            }
            else{
                System.out.print('*');
                int mid=2*(half-Math.abs(half-i))-1;
                for(int j=0;j<mid;j++){
                    System.out.print(' ');
                }
                System.out.print('*');
            }
            System.out.println();
        }

    }
    //3.斐波那契1,1,2,3,5,8,13,21
    public static int fibonacci1(int n) {
        if(n==1||n==2)
            return 1;
        else
            return fibonacci1(n-1)+fibonacci1(n-2);
    }
    public static int fibonacci2(int n){
        if(n==1||n==2)
            return 1;
        else{
            int a=1;
            int b=1;
            for(int i=3;i<=n;i++){
                int temp=a+b;
                a=b;
                b=temp;
            }
            return b;
        }
    }
    public static void hanoi(int n){
        hanoi(n,'A','B','C');
    }
    private static void hanoi(int n,char a,char b,char c)//a通过b把n个盘给c
    {
        if(n==0) return;
        hanoi(n-1,a,c,b);//a通过c把n-1个盘给b；
        System.out.println(a+"->"+c);//a把最大盘给c
        hanoi(n-1,b,a,c);//此时a，b的地位交换了，是一个BAC的n-1层汉诺塔
    }
}
