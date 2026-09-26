package bankaccount;

/*金额不能是负数或零。
取款和转账前需要校验密码。
余额不能被外部代码随意直接修改。
敏感信息不应该被完整暴露出去。*/
public class BankAccount {
    // TODO: 修改属性的可见性
    private String accountNumber; //账号
    private String accountHolder; //持有人
    private double balance; //余额
    private String password; //密码

    public BankAccount(String accountNumber, String accountHolder, double initialBalance, String password) {
        // TODO
        if(accountNumber == null){
            throw new IllegalArgumentException("账号不得为空");
        } else if (accountHolder==null) {
            throw new IllegalArgumentException("用户名不得为空");
        } else if (!validatePassword(password)) {
            throw new IllegalArgumentException("密码无效");
        } else if (!validateAmount(initialBalance)) {
            throw new IllegalArgumentException("输入的金额无效");
        }else {
            this.accountNumber=accountNumber;
            this.accountHolder=accountHolder;
            this.balance=initialBalance;
            this.password=password;
        }

    }

    public void deposit(double amount) {//存款
        // TODO
        if(amount>0){
            balance+=amount;
        }
        else {
            System.out.println("Invalid Number");
        }
    }

    public boolean withdraw(double amount, String inputPassword) {//取款
        // TODO
        if(!validateAmount(amount)||!validatePassword(inputPassword)){
            System.out.println("输入无效");
            return false;
        }
        if(!this.password.equals(inputPassword)){
            System.out.println("密码错误");
            return false;
        }
        if(amount<=0){
            System.out.println("金额无效");
            return false;
        }
        if(this.balance-amount<0){
            System.out.println("余额不足");
            return false;
        }
        this.balance-=amount;
        return true;
    }

    public boolean transfer(BankAccount recipient, double amount, String inputPassword) {//转钱
        // TODO
        if (recipient==null){
            System.out.println("账户不存在");
            return false;
        }
        if(!validateAmount(amount)||!validatePassword(inputPassword)){
            System.out.println("输入无效");
            return false;
        }
        if(!this.password.equals(inputPassword)){
            System.out.println("密码错误");
            return false;
        }
        if(amount<=0||this.balance-amount<0){
            System.out.println("余额不足");
            return false;
        }
        this.balance-=amount;
        recipient.balance+=amount;
        return true;
    }

    public double getBalance() {
        // TODO
        return balance;
    }

    public String getAccountInfo() {
        // TODO
        return "账号："+masknumber(accountNumber)+" 持有人："+maskholder(accountHolder);
    }

    // 只需修改可见性
    private boolean validatePassword(String inputPassword) {
        return true;
    }

    // 只需修改可见性
    private boolean validateAmount(double amount) {
        return true;
    }

    private String masknumber(String account){
        int len=account.length();
        if(len<=8)return "****";
        return account.substring(0,4)+"****"+account.substring(len-4,len);
    }
    private String maskholder(String account){
        return account.substring(0,1)+"**";
    }
}
