import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TradingBot extends TelegramLongPollingBot {
    private final BinanceClient binanceClient;

    public TradingBot() {
        String apiKey = "binance-api-key";
        String secretKey = "binance-secret-key";
        binanceClient = new BinanceClient(apiKey, secretKey);
    }

    @Override
    public String getBotUsername() {
        return "bot-username";
    }

    @Override
    public String getBotToken() {
        return "bot-token";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            handleIncomingMessage(messageText, chatId);
        }
    }

    private void handleIncomingMessage(String messageText, String chatId) {
        String response = processMessage(messageText);
        sendMessage(chatId, response);
    }

    private String processMessage(String messageText) {
        String[] parts = messageText.split(" ");
        String action = parts[0].toLowerCase();

        if (action.equals("balance")) {
            return binanceClient.accBalance();
        } else if (action.equals("positions")) {
            return binanceClient.posInfo();
        }

        if (parts.length == 3) {
            String symbol = parts[1].toUpperCase();
            double amount;
            try {
                amount = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                return "Invalid amount.";
            }

            String result;
            if (action.equals("long")) {
                result = binanceClient.placeOrder(symbol, "BUY", amount);
            } else if (action.equals("short")) {
                result = binanceClient.placeOrder(symbol, "SELL", amount);
            } else {
                return "Invalid action. Use 'long', 'short', 'balance', or 'positions'.";
            }

            // Check if the result is an error message or a successful order response
            if (result.startsWith("Error")) {
                return result;
            } else {
                return result;
            }
        } else {
            return "Invalid format. Use 'long SYMBOL AMOUNT', 'short SYMBOL AMOUNT', 'balance', or 'positions'.";
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
