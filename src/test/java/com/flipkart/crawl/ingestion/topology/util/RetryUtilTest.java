package com.flipkart.crawl.ingestion.topology.util;

import com.flipkart.crawl.ingestion.topology.model.RetryEnvelope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryUtilTest {

    private static final String PLAIN = "{\"product_id\":42,\"site_id\":1}";

    @Test
    void extractRaw_plain_returnsSame() {
        assertEquals(PLAIN, RetryUtil.extractRawEventJson(PLAIN));
    }

    @Test
    void extractRaw_envelope_returnsInner() throws Exception {
        String wrapped = RetryUtil.wrapWithAttempt(PLAIN, 1);
        assertEquals(PLAIN, RetryUtil.extractRawEventJson(wrapped));
    }

    @Test
    void currentAttempt_plain_isZero() {
        assertEquals(0, RetryUtil.currentAttempt(PLAIN));
    }

    @Test
    void currentAttempt_envelope_readsAttempt() throws Exception {
        String wrapped = RetryUtil.wrapWithAttempt(PLAIN, 2);
        assertEquals(2, RetryUtil.currentAttempt(wrapped));
    }

    @Test
    void wrapWithAttempt_roundTrip() throws Exception {
        String s = RetryUtil.wrapWithAttempt(PLAIN, 3);
        RetryEnvelope e = RetryUtil.parse(s);
        assertEquals(PLAIN, e.getRawEventJson());
        assertEquals(3, e.getAttempt());
    }

    @Test
    void looksLikeEnvelope_falseForPlainCrawl() {
        assertFalse(RetryUtil.looksLikeEnvelope(PLAIN));
    }

    @Test
    void looksLikeEnvelope_trueForEnvelope() throws Exception {
        assertTrue(RetryUtil.looksLikeEnvelope(RetryUtil.wrapWithAttempt(PLAIN, 1)));
    }
}
