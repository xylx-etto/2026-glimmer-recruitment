package shapeabstract;

public class Main {
    public static void main(String[] args){
        Shape[] shapes={new Circle(2),new Rectangle(3,4),new Triangle(3,4,5)};
        for (Shape s :shapes){
            System.out.println(s.id()+"的周长="+s.perimeter()+", 面积="+s.area());
        }
    }

}
//抽象类实现
//未进行数据是否合法的检验，仅作为数据合法情况下的实现
class Circle extends Shape{

    public Circle(double r){super(r);}
    @Override
    public String id(){return "圆";}
    @Override
    public double perimeter() {
        return 2*Math.PI*a;
    }
    @Override
    public double area() {
        return Math.PI*a*a;
    }
}

class Rectangle extends Shape{
    public Rectangle(double a,double b){super(a,b);}
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

class Triangle extends Shape{
    public Triangle(double a,double b,double c){super(a,b,c);}
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
