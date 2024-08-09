package eu.avalanche7.forgeannouncements.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern URL_PATTERN = Pattern.compile("http://\\S+|https://\\S+");

    public static MutableComponent parseMessageWithColor(String rawMessage) {
        rawMessage = rawMessage.replace("&", "§");

        Matcher hexMatcher = HEX_PATTERN.matcher(rawMessage);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            if (isValidHexColor(hexColor)) {
                hexMatcher.appendReplacement(sb, "§#" + hexColor);
            }
        }
        hexMatcher.appendTail(sb);
        rawMessage = sb.toString();

        MutableComponent message = Component.empty();
        String[] parts = rawMessage.split("(?=§)");
        Style currentStyle = Style.EMPTY;

        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.startsWith("§#")) {
                String hexCode = part.substring(2, 8);
                if (isValidHexColor(hexCode)) {
                    try {
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(Integer.parseInt(hexCode, 16)));
                    } catch (NumberFormatException e) {
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(0xFFFFFF));
                    }
                    part = part.substring(8);
                }
            } else if (part.startsWith("§")) {
                char code = part.charAt(1);
                ChatFormatting format = ChatFormatting.getByCode(code);
                if (format != null) {
                    currentStyle = currentStyle.applyFormat(format);
                }
                part = part.substring(2);
            }

            Matcher urlMatcher = URL_PATTERN.matcher(part);
            int lastEnd = 0;
            MutableComponent partComponent = Component.empty();

            while (urlMatcher.find()) {
                partComponent.append(Component.literal(part.substring(lastEnd, urlMatcher.start()))
                        .setStyle(currentStyle));

                String url = urlMatcher.group();
                partComponent.append(Component.literal(url)
                        .setStyle(currentStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));

                lastEnd = urlMatcher.end();
            }
            partComponent.append(Component.literal(part.substring(lastEnd))
                    .setStyle(currentStyle));

            message.append(partComponent);
        }

        return message;
    }

    private static boolean isValidHexColor(String hexColor) {
        return hexColor.matches("[A-Fa-f0-9]{6}");
    }
}