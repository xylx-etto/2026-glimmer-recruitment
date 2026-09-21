import java.io.*;
import java.nio.charset.StandardCharsets;
public class Task3 {
    public static void main(String[] args) {
        double sum=0;
        int line=0;
        double count=0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data.txt"), StandardCharsets.UTF_8))){
            String s;
            if((s= br.readLine())==null){
                throw new EmptyFileException("文件为空");   //自定义异常类判定条件
            }
            while ((s)!=null)
            {

                try {
                    line++;
                    count++;
                    sum+=Integer.parseInt(s);
                }catch (NumberFormatException e){
                    System.out.println("在第"+line+"行出现了无法解析为整数的内容");
                    count--;
                }
                s= br.readLine();
            }
            if(count!=0){
                System.out.println(sum/count);
            }
            else{
                System.out.println("无有效数据，无法计算");
            }
        }catch (EmptyFileException e){
            System.out.println("文件为空");
        }catch (FileNotFoundException e) {
            System.out.println("文件不存在");
            throw new RuntimeException(e);
        }catch (IOException e) {
            System.out.println("文件无法读取");
            throw new RuntimeException(e);
        }
    }
    public static class EmptyFileException extends RuntimeException{    //创建自定义异常类
        public EmptyFileException(String s)
        {
            super(s);
        }
    }
}