package shapeinterface;

public class Main {
    public static void main(String[] args) {
        Shape[] shapes={new Circle(2),new Rectangle(3,4),new Triangle(3,4,5)};
        for (Shape s :shapes){
            System.out.println(s.id()+"的周长="+s.perimeter()+", 面积="+s.area());
        }
    }
}
//接口方法
class Circle implements Shape {
    double r;
    public Circle(double r){
        this.r=r;
    }
    @Override
    public String id(){return "圆";}
    @Override
    public double perimeter() {
        return 2*Math.PI*r;
    }
    @Override
    public double area() {
        return Math.PI*r*r;
    }
}

class Rectangle implements Shape {
    double a,b;
    public Rectangle(double a,double b){
        this.a=a;
        this.b=b;
    }
    @Override
    public String id(){return "矩形";}
    @Override
    public double perimeter() {
        return 2*a+2*b;
    }
    @Override
    public double area() {
        return a*b;
    }
}

class Triangle implements Shape {
    double a,b,c;
    public Triangle(double a,double b,double c){
        this.a=a;
        this.b=b;
        this.c=c;
    }
    @Override
    public String id(){return "三角形";}
    @Override
    public double perimeter() {
        return a+b+c;
    }
    @Override
    public double area() {
        double p=perimeter()/2;
        return Math.sqrt(p*(p-a)*(p-b)*(p-c));
    }
}