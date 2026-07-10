package com.apexads.sdk.core.tracking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;

/**
 * OpenRTB substitution-macro expansion for event notice URLs (nurl/burl/lurl).
 *
 * <p>Exchanges and DSPs settle billing off {@code ${AUCTION_PRICE}}; sending a notice
 * with the literal macro intact is a dropped event on the bidder side. Values are
 * URL-encoded per the OpenRTB spec. Unknown macros are left untouched — they belong
 * to someone else's expansion step.</p>
 */
public final class AuctionMacros {

    /** OpenRTB loss reason: lost to a higher bid in the same auction. */
    public static final int LOSS_LOST_TO_HIGHER_BID = 102;

    private AuctionMacros() {}

    @Nullable
    public static String expand(@Nullable String url,
                                @Nullable String auctionId,
                                @Nullable String impId,
                                @Nullable String bidId,
                                double price,
                                @Nullable String currency) {
        if (url == null || url.isEmpty()) return url;
        String out = url;
        out = replace(out, "${AUCTION_ID}", auctionId);
        out = replace(out, "${AUCTION_IMP_ID}", impId);
        out = replace(out, "${AUCTION_BID_ID}", bidId);
        out = replace(out, "${AUCTION_PRICE}", formatPrice(price));
        out = replace(out, "${AUCTION_CURRENCY}", currency);
        return out;
    }

    /**
     * Expands a loss notice ({@code lurl}). {@code price} is the settlement price of the
     * auction (the winning bid), which is what {@code ${AUCTION_PRICE}} means in a loss
     * context per OpenRTB.
     */
    @Nullable
    public static String expandLoss(@Nullable String url,
                                    @Nullable String auctionId,
                                    @Nullable String impId,
                                    @Nullable String bidId,
                                    double price,
                                    @Nullable String currency,
                                    int lossReason) {
        String out = expand(url, auctionId, impId, bidId, price, currency);
        if (out == null || out.isEmpty()) return out;
        return replace(out, "${AUCTION_LOSS}", Integer.toString(lossReason));
    }

    @NonNull
    static String formatPrice(double price) {
        BigDecimal value = BigDecimal.valueOf(price);
        if (value.signum() == 0) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private static String replace(String url, String macro, @Nullable String value) {
        if (!url.contains(macro)) return url;
        return url.replace(macro, encode(value == null ? "" : value));
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is guaranteed on Android; keep the raw value rather than drop the event.
            return value;
        }
    }
}
