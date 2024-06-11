import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class BinanceClient {
    private final UMFuturesClientImpl futuresClient;
    private final ObjectMapper objectMapper;

    public BinanceClient(String apiKey, String secretKey) {
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("API key and Secret key cannot be null or empty");
        }
        futuresClient = new UMFuturesClientImpl(apiKey, secretKey);
        objectMapper = new ObjectMapper();
    }

    public String placeOrder(String symbol, String side, double quantity) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("side", side);
        parameters.put("type", "MARKET");
        parameters.put("quantity", String.valueOf(quantity));

        try {
            String response = futuresClient.account().newOrder(parameters);
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            String origQty = responseMap.get("origQty").toString();
            String lastPrice = getLastPrice(symbol);
            return String.format("Position opened: %s\nSize: %s\nEstimated Average Price: %s USDT", symbol, origQty, lastPrice);
        } catch (BinanceConnectorException e) {
            e.printStackTrace();
            return "Connector error: " + e.getMessage();
        } catch (BinanceClientException e) {
            e.printStackTrace();
            return "Client error: " + e.getErrMsg() + " (code: " + e.getErrorCode() + ")";
        } catch (BinanceServerException e) {
            e.printStackTrace();
            return "Server error: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }

    public String accBalance() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        try {
            String response = futuresClient.account().futuresAccountBalance(parameters);
            List<Map<String, Object>> responseList = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> assetInfo : responseList) {
                if ("USDT".equals(assetInfo.get("asset"))) {
                    return String.format(
                            "Asset: %s\nBalance: %s\nCross Wallet Balance: %s\nCross Unrealized PnL: %s\nAvailable Balance: %s\nMax Withdraw Amount: %s",
                            assetInfo.get("asset"),
                            assetInfo.get("balance"),
                            assetInfo.get("crossWalletBalance"),
                            assetInfo.get("crossUnPnl"),
                            assetInfo.get("availableBalance"),
                            assetInfo.get("maxWithdrawAmount")
                    );
                }
            }
            return "YOU'RE FUCKING POOR!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching balance: " + e.getMessage();
        }
    }

    public String posInfo() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        try {
            String response = futuresClient.account().positionInformation(parameters);
            List<Map<String, Object>> positions = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            StringBuilder sb = new StringBuilder("Open positions:\n\n");
            for (Map<String, Object> position : positions) {
                String positionAmtStr = position.get("positionAmt").toString();
                double positionAmt = Double.parseDouble(positionAmtStr);
                if (positionAmt != 0) {
                    double entryPrice = Double.parseDouble(position.get("entryPrice").toString());
                    double markPrice = Double.parseDouble(position.get("markPrice").toString());
                    double unrealizedProfit = (markPrice - entryPrice) * positionAmt;

                    sb.append(String.format(
                            "Symbol: %s\nPosition: %s\nUnrealized PnL: %.2f USDT\nEntry Price: %.2f\nMark Price: %.2f\nLiquidation Price: %s\n\n",
                            position.get("symbol"),
                            positionAmtStr,
                            unrealizedProfit,
                            entryPrice,
                            markPrice,
                            position.get("liquidationPrice").toString()
                    ));
                }
            }
            if (sb.length() == 0) {
                return "No open positions. HOW ABOUT SOME GAMBLING BRO";
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching position information: " + e.getMessage();
        }
    }

    private String getLastPrice(String symbol) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        try {
            String response = futuresClient.market().markPrice(parameters);
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            return responseMap.get("markPrice").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
}
