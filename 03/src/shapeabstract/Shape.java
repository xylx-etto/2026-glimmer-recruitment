package shapeabstract;

public abstract class Shape {
    protected double a,b,c;

    public Shape(){}
    public Shape(double r){ //圆形的构造
        this.a=r;//半径
    }
    public Shape(double a,double b){//长方形构造
        this.a=a;
        this.b=b;//长宽
    }
    public Shape(double a,double b, double c){//三角形构造
        this.a=a;
        this.b=b;
        this.c=c;//三边长
    }
    public abstract String id();
    public abstract double perimeter();//计算周长
    public abstract double area();//计算面积
}
