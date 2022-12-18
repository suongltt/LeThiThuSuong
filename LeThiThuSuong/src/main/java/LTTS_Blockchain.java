import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
public class LTTS_Blockchain {
    public static ArrayList<VNPT_Suong> blockchain = new ArrayList<VNPT_Suong>();
    public static int difficulty = 5;
    public static float minimumTransaction = 0.1f;
    public static Store LTTS_Kho; //Kho
    public static Store LTTS_Cuahang; //Cửa hàng
    public static Transaction genesisTransaction;
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
    public static void main(String[] args) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Scanner sc = new Scanner(System.in);
        LTTS_Kho = new Store();
        LTTS_Cuahang = new Store();
        Store coinbase = new Store();
        Transaction sendFund;
        System.out.print("Số lượng VNPT-Net Router trong kho : ");
        float initBalanceA = sc.nextFloat();
        genesisTransaction = new Transaction(coinbase.publicKey, LTTS_Kho.publicKey, initBalanceA, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        VNPT_Suong genesis = new VNPT_Suong("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Số lượng VNPT-Net Router trong kho là : " + LTTS_Kho.getBalance());
        System.out.print("Số lượng VNPT-Net Router của Cửa hàng đang còn : ");
        float initBalanceB = sc.nextFloat();
        genesisTransaction = new Transaction(coinbase.publicKey, LTTS_Cuahang.publicKey, initBalanceB, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);
        System.out.println("Số lượng VNPT-Net Router của Cửa hàng đang còn là : " + LTTS_Cuahang.getBalance());
        VNPT_Suong block1 = new VNPT_Suong(genesis.hash);
        boolean fail = true;
        while (fail){
            System.out.print(" Nhập số lượng VNPT-Net Router cần chuyển từ kho sang cửa hàng : ");
            float numberTransfer = sc.nextFloat();
            System.out.println("Đang xử lý ........................");
            sendFund = LTTS_Kho.sendFunds(LTTS_Cuahang.publicKey, numberTransfer);
            if (sendFund==null){
                continue;
            }else{
                fail= false;
                block1.addTransaction(sendFund);
            }
        }
        addBlock(block1);
        System.out.println("Số lượng VNPT-Net Router mới trong kho và cửa hàng sau khi chuyển: ");
        System.out.println("Số lượng VNPT-Net Router trong kho hiện tại là : " + LTTS_Kho.getBalance());
        System.out.println("Số lượng VNPT-Net Router của Cửa hàng hiện tại là : " + LTTS_Cuahang.getBalance());
    }
    public static Boolean isChainValid() {
        VNPT_Suong currentBlock;
        VNPT_Suong previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>();
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }
                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }
                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }
                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }
                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }
            }
        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_Suong newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
