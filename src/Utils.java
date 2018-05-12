import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.TimeInForce;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;

public class Utils {

    static Boolean isTrading = false;

    static BigDecimal x1 = new BigDecimal("0.01");
    static BigDecimal xx1 = new BigDecimal("0.001");

    static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("apikey", "secret");
    static BinanceApiAsyncRestClient client = factory.newAsyncRestClient();
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    //ʵ�ʳɽ���õ���Ԥ�ڵ�99.8%
    static BigDecimal amoutRate = new BigDecimal("0.997");


    static void doRoute(String tokenName,
                        BigDecimal TOKENBTCbidLocal, BigDecimal TOKENBTCbidVolLocal, BigDecimal TOKENBTCaskLocal, BigDecimal TOKENBTCaskVolLocal,
                        BigDecimal TOKENETHbidLocal, BigDecimal TOKENETHbidVolLocal, BigDecimal TOKENETHaskLocal, BigDecimal TOKENETHaskVolLocal) {

        if (Storage.ETHBTCbidVol.compareTo(BigDecimal.ZERO) <= 0 || Storage.ETHBTCaskVol.compareTo(BigDecimal.ZERO) <= 0)
            return;

        if (TOKENBTCaskLocal.compareTo(BigDecimal.ZERO) <= 0 || TOKENBTCaskVolLocal.compareTo(BigDecimal.ZERO) <= 0 || TOKENETHbidLocal.compareTo(BigDecimal.ZERO) <= 0 || TOKENETHbidVolLocal.compareTo(BigDecimal.ZERO) <= 0)
            return;

        long timeStamp = Storage.ETHBTCTime;
//        System.out.println("--------------start----" + sdf.format(new Date(timeStamp)) + "---------------");
//        System.out.println("ETH-BTC/bid:" + Storage.ETHBTCbid + " | " + Storage.ETHBTCbidVol + " , " + tokenName + "-BTC/ask:" + TOKENBTCaskLocal + " | " + TOKENBTCaskVolLocal + " , " + tokenName + "-ETH/bid:" + TOKENETHbidLocal + " | " + TOKENETHbidVolLocal);
        Utils.checkRoute("ETH", Storage.ETHBTCbid, Utils.Trade.BID, Storage.ETHBTCbidVol,
                "BTC", TOKENBTCaskLocal, Utils.Trade.ASK, TOKENBTCaskVolLocal,
                tokenName, TOKENETHbidLocal, Utils.Trade.BID, TOKENETHbidVolLocal, timeStamp);

        Utils.checkRoute("ETH", TOKENETHaskLocal, Utils.Trade.ASK, TOKENETHaskVolLocal,
                tokenName, TOKENBTCbidLocal, Utils.Trade.BID, TOKENBTCbidVolLocal,
                "BTC", Storage.ETHBTCask, Utils.Trade.ASK, Storage.ETHBTCaskVol, timeStamp);

        Utils.checkRoute("BTC", TOKENBTCaskLocal, Utils.Trade.ASK, TOKENBTCaskVolLocal,
                tokenName, TOKENETHbidLocal, Utils.Trade.BID, TOKENETHbidVolLocal,
                "ETH", Storage.ETHBTCbid, Utils.Trade.BID, Storage.ETHBTCbidVol, timeStamp);

        Utils.checkRoute("BTC", Storage.ETHBTCask, Utils.Trade.ASK, Storage.ETHBTCaskVol,
                "ETH", TOKENETHaskLocal, Utils.Trade.ASK, TOKENETHaskVolLocal,
                tokenName, TOKENBTCbidLocal, Utils.Trade.BID, TOKENBTCbidVolLocal, timeStamp);

        Utils.checkRoute(tokenName, TOKENETHbidLocal, Utils.Trade.BID, TOKENETHbidVolLocal,
                "ETH", Storage.ETHBTCbid, Utils.Trade.BID, Storage.ETHBTCbidVol,
                "BTC", TOKENBTCaskLocal, Utils.Trade.ASK, TOKENBTCaskVolLocal, timeStamp);

        Utils.checkRoute(tokenName, TOKENBTCbidLocal, Utils.Trade.BID, TOKENBTCbidVolLocal,
                "BTC", Storage.ETHBTCask, Utils.Trade.ASK, Storage.ETHBTCaskVol,
                "ETH", TOKENETHaskLocal, Utils.Trade.ASK, TOKENETHaskLocal, timeStamp);

    }

    static void checkRoute(String A, BigDecimal pA, Utils.Trade type1, BigDecimal vA,
                           String B, BigDecimal pB, Utils.Trade type2, BigDecimal vB,
                           String C, BigDecimal pC, Utils.Trade type3, BigDecimal vC,
                           long timeStamp) {

        //calculate through the trade, diminish previous trade volumes if volume bottlenecks are found
        //only do this calculation if the 1.0f test works, saving processing power
        BigDecimal amountA = (type1.equals(Trade.BID) ? vA : vA.multiply(pA));

        BigDecimal amountB = (type1.equals(Trade.BID) ? amountA.multiply(pA) : vA);

        //find a ratio between possible trade size for next step and what current amount is
        BigDecimal volRatioBC = (type2.equals(Trade.ASK) ? vB.multiply(pB) : vB).divide(amountB, 8, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal amountC;
        if (volRatioBC.compareTo(BigDecimal.ONE) < 0) {
            //reduce the size of the entire trade based on volume bottleneck
            amountA = amountA.multiply(volRatioBC);
            amountB = amountB.multiply(volRatioBC);
            amountC = (type2.equals(Trade.BID) ? vB.multiply(pB) : vB);
        } else {
            amountC = (type2.equals(Trade.BID) ? amountB.multiply(pB) : amountB.divide(pB, 3, BigDecimal.ROUND_HALF_EVEN));
        }

        if (amountC.compareTo(BigDecimal.ZERO) <= 0)
            return;

        BigDecimal volRatioCA = (type3.equals(Trade.ASK) ? vC.multiply(pC) : vC).divide(amountC, 3, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal amountAFinal;
        if (volRatioCA.compareTo(BigDecimal.ONE) < 0) {
            amountA = amountA.multiply(volRatioCA);
            amountB = amountB.multiply(volRatioCA);
            amountC = amountC.multiply(volRatioCA);
            amountAFinal = (type3.equals(Trade.BID) ? vC.multiply(pC) : vC);
        } else {
            amountAFinal = (type3.equals(Trade.BID) ? amountC.multiply(pC) : amountC.divide(pC, 3, BigDecimal.ROUND_HALF_EVEN));
        }


        if (A.equals("BTC") && B.equals("ETH")) {
            // BTC -> ETH ����3λС��
            amountB = amountB.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 3, BigDecimal.ROUND_HALF_EVEN);
            // ETH -> TOKEN  ����2λС��
            amountC = amountC.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            // TOKEN -> BTC ����0λС��
            amountAFinal = amountAFinal.setScale(0, BigDecimal.ROUND_HALF_EVEN);
        } else if (A.equals("BTC") && C.equals("ETH")) {
            // BTC -> TOKEN ����0λС��
            amountB = amountB.setScale(0, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 0, BigDecimal.ROUND_HALF_EVEN);
            // TOKEN -> ETH  ����2λС��
            amountC = amountC.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            // ETH -> BTC ����3λС��
            amountAFinal = amountAFinal.setScale(3, BigDecimal.ROUND_HALF_EVEN);
        } else if (A.equals("ETH") && B.equals("BTC")) {
            // ETH -> BTC ����3λС��
            amountB = amountB.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 3, BigDecimal.ROUND_HALF_EVEN);
            // BTC -> TOKEN ����0λС��
            amountC = amountC.setScale(0, BigDecimal.ROUND_HALF_EVEN);
            // TOKEN -> ETH  ����2λС��
            amountAFinal = amountAFinal.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        } else if (A.equals("ETH") && C.equals("BTC")) {
            // ETH -> TOKEN  ����2λС��
            amountB = amountB.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 2, BigDecimal.ROUND_HALF_EVEN);
            // TOKEN -> BTC ����0λС��
            amountC = amountC.setScale(0, BigDecimal.ROUND_HALF_EVEN);
            // BTC -> ETH ����3λС��
            amountAFinal = amountAFinal.setScale(3, BigDecimal.ROUND_HALF_EVEN);
        } else if (B.equals("ETH") && C.equals("BTC")) {
            // TOKEN -> ETH  ����2λС��
            amountB = amountB.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 2, BigDecimal.ROUND_HALF_EVEN);
            // ETH -> BTC ����3λС��
            amountC = amountC.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            // BTC -> TOKEN ����0λС��
            amountAFinal = amountAFinal.setScale(0, BigDecimal.ROUND_HALF_EVEN);
        } else if (B.equals("BTC") && C.equals("ETH")) {
            // TOKEN -> BTC ����0λС��
            amountB = amountB.setScale(0, BigDecimal.ROUND_HALF_EVEN);
            amountA = amountB.divide(pA, 0, BigDecimal.ROUND_HALF_EVEN);
            // BTC -> ETH ����3λС��
            amountC = amountC.setScale(3, BigDecimal.ROUND_HALF_EVEN);
            // ETH -> TOKEN  ����2λС��
            amountAFinal = amountAFinal.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        } else {
            System.out.printf("��Ч�Ľ��׶ԣ��˳���");
            return;
        }

        if (amountA.compareTo(BigDecimal.ZERO) > 0 && amountAFinal.compareTo(BigDecimal.ZERO) > 0 && amountAFinal.compareTo(amountA) > 0) {
            BigDecimal fee = amountA.multiply(new BigDecimal("0.0015"));//�����ѷ���
            BigDecimal income = amountAFinal.subtract(amountA).subtract(fee);
            if ((amountA.add(fee)).compareTo(amountAFinal) > 0) {
//                System.out.println("α��������: " + amountA + " to " + amountAFinal + " in " + A + "��������: " + income);
                return;
            } else if (income.compareTo(fee.multiply(new BigDecimal(2))) < 0) {
//                System.out.println("������ͣ������ѣ�" + fee + "�����棺" + income);
                return;
            } else {
                boolean isMinAmount = true;
                if (A.equals("BTC") && B.equals("ETH")) {
                    if (amountA.compareTo(xx1) < 0 || amountB.compareTo(x1) < 0 || amountAFinal.compareTo(xx1) < 0) {
                        isMinAmount = false;
                    }
                } else if (A.equals("BTC") && C.equals("ETH")) {
                    if (amountA.compareTo(xx1) < 0 || amountC.compareTo(x1) < 0 || amountAFinal.compareTo(xx1) < 0) {
                        isMinAmount = false;
                    }
                } else if (A.equals("ETH") && B.equals("BTC")) {
                    if (amountA.compareTo(x1) < 0 || amountB.compareTo(xx1) < 0 || amountAFinal.compareTo(x1) < 0) {
                        isMinAmount = false;
                    }
                } else if (A.equals("ETH") && C.equals("BTC")) {
                    if (amountA.compareTo(x1) < 0 || amountC.compareTo(xx1) < 0 || amountAFinal.compareTo(x1) < 0) {
                        isMinAmount = false;
                    }
                } else if (B.equals("ETH") && C.equals("BTC")) {
                    if (amountB.compareTo(x1) < 0 || amountC.compareTo(xx1) < 0 || amountAFinal.compareTo(xx1) < 0) {
                        isMinAmount = false;
                    }
                } else if (B.equals("BTC") && C.equals("ETH")) {
                    if (amountB.compareTo(xx1) < 0 || amountC.compareTo(x1) < 0 || amountAFinal.compareTo(x1) < 0) {
                        isMinAmount = false;
                    }
                }

                if (!isMinAmount) {
//                    System.out.println("ʧ�ܣ�����" + amountA + A + "->" + amountB + B + "->" + amountC + C + "->" + amountAFinal + A + ",��������͹��������ʧ�ܣ�");
                    return;
                }


                System.out.println("��������: " + amountA + " to " + amountAFinal + " in " + A);
                System.out.println("���׹켣" + amountA + A + "->" + amountB + B + "->" + amountC + C + "->" + amountAFinal + A);
                System.out.println("���׼۸�" + pA + A + "->" + pB + B + "->" + pC + C);
                System.out.println("������:" + fee + A);

                BigDecimal realAmoutB = type1.equals(Trade.BID) ? amountA.multiply(pA) : amountA.divide(pA, 8, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal amoutBDiff = realAmoutB.subtract(amountB);
                System.out.println("��һ�ν��ײ��"+amoutBDiff+B);

                BigDecimal realAmoutC = type2.equals(Trade.BID) ? amountB.multiply(pB) : amountB.divide(pB, 8, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal realAmoutCDiff = type2.equals(Trade.BID) ? amoutBDiff.multiply(pB) : amoutBDiff.divide(pB, 8, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal amoutCDiff = realAmoutC.subtract(amountC);
                System.out.println("�ڶ��ν��ײ��"+amoutCDiff+C);
                amoutCDiff = realAmoutCDiff.add(amoutCDiff);
                System.out.println("�ڶ������һ�β���ۼӺ��ײ��"+amoutCDiff+C);


                BigDecimal realAmoutA = type3.equals(Trade.BID) ? amountC.multiply(pC) : amountC.divide(pC, 8, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal realAmoutADiff = type3.equals(Trade.BID) ? amoutCDiff.multiply(pC) : amoutCDiff.divide(pC, 8, BigDecimal.ROUND_HALF_EVEN);
                BigDecimal amoutADiff = realAmoutA.subtract(amountAFinal);
                System.out.println("�����ν��ײ��"+amoutADiff+A);
                amoutADiff = realAmoutADiff.add(amoutADiff);
                System.out.println("��������ǰ���β���ۼӺ��ײ��"+amoutADiff+A);
                amoutADiff = amoutADiff.subtract(fee);
                System.out.println("Ԥ�ڻ���: " + income + A);
                System.out.println("ʵ������: " + amoutADiff + A);

                if (amoutADiff.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }

                BigDecimal BTCINCOME = new BigDecimal(0);
                BigDecimal ETHINCOME = new BigDecimal(0);
                BigDecimal TOKENINCOME = new BigDecimal(0);

                if (A.equals("BTC")) {
                    BTCINCOME = BTCINCOME.add(amoutADiff);
                } else if (A.equals("ETH")) {
                    ETHINCOME = ETHINCOME.add(amoutADiff);
                } else {
                    TOKENINCOME = TOKENINCOME.add(amoutADiff);
                }

                System.out.println("����" + type1 + A + "->" + B + ": " + amountA + A + " to " + amountB + B + " �۸� " + pA);
                System.out.println("����" + type2 + B + "->" + C + ": " + amountB + B + " to " + amountC + C + " �۸� " + pB);
                System.out.println("����" + type3 + C + "->" + A + ": " + amountC + C + " to " + amountAFinal + A + " �۸� " + pC);

                synchronized (isTrading) {
                    if (isTrading) {
                        System.out.println("���ڽ��ף�����");
                        return;
                    }
                    isTrading = true;
                }

                if (type1.equals(Trade.BID)) {
                    System.out.println("���׶ԣ�" + A + B);
                    client.newOrder(limitSell(A + B, TimeInForce.GTC, amountA.toString(), pA.toString()), response -> System.out.println("sell order success��"));
                } else {
                    System.out.println("���׶ԣ�" + B + A);
                    client.newOrder(limitBuy(B + A, TimeInForce.GTC, amountB.toString(), pA.toString()), response -> System.out.println("buy order success!"));
                }


                if (type2.equals(Trade.BID)) {
                    System.out.println("���׶ԣ�" + B + C);
                    client.newOrder(limitSell(B + C, TimeInForce.GTC, amountB.toString(), pB.toString()), response -> System.out.println("sell order success��"));
                } else {
                    System.out.println("���׶ԣ�" + C + B);
                    client.newOrder(limitBuy(C + B, TimeInForce.GTC, amountC.toString(), pB.toString()), response -> System.out.println("buy order success!"));
                }


                if (type3.equals(Trade.BID)) {
                    System.out.println("���׶ԣ�" + C + A);
                    client.newOrder(limitSell(C + A, TimeInForce.GTC, amountC.toString(), pC.toString()), response -> System.out.println("sell order success��"));
                } else {
                    System.out.println("���׶ԣ�" + A + C);
                    client.newOrder(limitBuy(A + C, TimeInForce.GTC, amountAFinal.toString(), pC.toString()), response -> System.out.println("buy order success!"));
                }



                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                System.out.println("����ʱ�䣺" + sdf.format(new Date(timeStamp)) + "\n");
                System.out.println("������ܣ�BTCINCOME��" + BTCINCOME + " | ETHINCOME��" + ETHINCOME + " | " + "TOKENINCOME��" + TOKENINCOME);


                try {
                    //��ͣ3��
                    Thread.sleep(30000l);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (isTrading) {
                    System.exit(0);
                    isTrading = false;
                }

            }


            //for testing differences
//				System.out.println("BNBBTCbid" + Storage.BNBBTCbid);
//				System.out.println("BNBBTCbidVol" + Storage.BNBBTCbidVol);
//				System.out.println("BNBBTCask" + Storage.BNBBTCask);
//				System.out.println("BNBBTCaskVol" + Storage.BNBBTCaskVol);
//
//				System.out.println("BNBETHbid" + Storage.BNBETHbid);
//				System.out.println("BNBETHbidVol" + Storage.BNBETHbidVol);
//				System.out.println("BNBETHask" + Storage.BNBETHask);
//				System.out.println("BNBETHaskVol" + Storage.BNBETHaskVol);
//
//				System.out.println("ETHBTCbid" + Storage.ETHBTCbid);
//				System.out.println("ETHBTCbidVol" + Storage.ETHBTCbidVol);
//				System.out.println("ETHBTCask" + Storage.ETHBTCask);
//				System.out.println("ETHBTCaskVol" + Storage.ETHBTCaskVol);
        } else {
//                if (amountA.compareTo(BigDecimal.ZERO) > 0)
//                    System.out.println(A + "->" + B + "->" + C + "->" + A + " losing yield:" + (amountAFinal.divide(amountA, 6, BigDecimal.ROUND_HALF_EVEN)));
        }

    }

    public static enum Trade {
        BID, ASK
    }

}
