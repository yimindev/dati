package com.dati.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HashUtils unit tests")
class HashUtilsTest {

    @Test
    void sha256HexMatchesKnownVectors() {
        assertThat(HashUtils.sha256Hex("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(HashUtils.sha256Hex(""))
            .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void sha256HexIsStableAndCaseSensitive() {
        assertThat(HashUtils.sha256Hex("sk_test")).isEqualTo(HashUtils.sha256Hex("sk_test"));
        assertThat(HashUtils.sha256Hex("sk_test")).isNotEqualTo(HashUtils.sha256Hex("SK_test"));
    }
}
