package space.alnovis.protowrapper.it.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import space.alnovis.protowrapper.it.model.api.BankTransfer;
import space.alnovis.protowrapper.it.model.api.CreditCard;
import space.alnovis.protowrapper.it.model.api.Crypto;
import space.alnovis.protowrapper.it.model.api.Payment;
import space.alnovis.protowrapper.it.model.api.VersionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for oneof field support.
 *
 * Tests cover:
 * - Case enum generation and values
 * - Discriminator method (getMethodCase)
 * - Individual field has methods (hasCreditCard, hasBankTransfer, hasCrypto)
 * - Field accessors (getCreditCard, getBankTransfer, getCrypto)
 * - Builder methods (setCreditCard, setBankTransfer, setCrypto, clearMethod)
 * - Version conversion (v1 has 2 options, v2 has 3 options)
 */
@DisplayName("Oneof Field Support")
public class OneofIntegrationTest {

    @Nested
    @DisplayName("Case Enum")
    class CaseEnumTests {

        @Test
        @DisplayName("Case enum has correct constants")
        void caseEnumConstants() {
            // Verify enum constants exist
            assertNotNull(Payment.MethodCase.CREDIT_CARD);
            assertNotNull(Payment.MethodCase.BANK_TRANSFER);
            assertNotNull(Payment.MethodCase.CRYPTO);
            assertNotNull(Payment.MethodCase.METHOD_NOT_SET);
        }

        @Test
        @DisplayName("Case enum forNumber returns correct values")
        void caseEnumForNumber() {
            assertEquals(Payment.MethodCase.CREDIT_CARD, Payment.MethodCase.forNumber(10));
            assertEquals(Payment.MethodCase.BANK_TRANSFER, Payment.MethodCase.forNumber(11));
            assertEquals(Payment.MethodCase.CRYPTO, Payment.MethodCase.forNumber(12));
            assertEquals(Payment.MethodCase.METHOD_NOT_SET, Payment.MethodCase.forNumber(0));
            assertEquals(Payment.MethodCase.METHOD_NOT_SET, Payment.MethodCase.forNumber(999));
        }

        @Test
        @DisplayName("Case enum getNumber returns correct values")
        void caseEnumGetNumber() {
            assertEquals(10, Payment.MethodCase.CREDIT_CARD.getNumber());
            assertEquals(11, Payment.MethodCase.BANK_TRANSFER.getNumber());
            assertEquals(12, Payment.MethodCase.CRYPTO.getNumber());
            assertEquals(0, Payment.MethodCase.METHOD_NOT_SET.getNumber());
        }
    }

    @Nested
    @DisplayName("V1 Payment (Credit Card and Bank Transfer only)")
    class V1PaymentTests {

        @Test
        @DisplayName("Payment with credit card - v1")
        void paymentWithCreditCardV1() {
            VersionContext ctx = VersionContext.forVersion(1);
            CreditCard card = ctx.newCreditCardBuilder()
                    .setCardNumber("4111111111111111")
                    .setExpiry("12/25")
                    .setHolderName("John Doe")
                    .build();

            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-001")
                    .setAmount(10000L)
                    .setDescription("Test payment")
                    .setCreditCard(card)
                    .build();

            assertEquals(1, payment.getWrapperVersion());
            assertEquals("PAY-001", payment.getId());
            assertEquals(10000L, payment.getAmount());
            assertEquals("Test payment", payment.getDescription());

            // Check oneof discriminator
            assertEquals(Payment.MethodCase.CREDIT_CARD, payment.getMethodCase());

            // Check has methods
            assertTrue(payment.hasCreditCard());
            assertFalse(payment.hasBankTransfer());
            assertFalse(payment.hasCrypto());

            // Check credit card data
            CreditCard retrievedCard = payment.getCreditCard();
            assertNotNull(retrievedCard);
            assertEquals("4111111111111111", retrievedCard.getCardNumber());
            assertEquals("12/25", retrievedCard.getExpiry());
            assertEquals("John Doe", retrievedCard.getHolderName());
        }

        @Test
        @DisplayName("Payment with bank transfer - v1")
        void paymentWithBankTransferV1() {
            VersionContext ctx = VersionContext.forVersion(1);
            BankTransfer transfer = ctx.newBankTransferBuilder()
                    .setAccountNumber("123456789")
                    .setBankCode("SWIFT123")
                    .setBankName("Test Bank")
                    .build();

            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-002")
                    .setAmount(20000L)
                    .setBankTransfer(transfer)
                    .build();

            assertEquals(Payment.MethodCase.BANK_TRANSFER, payment.getMethodCase());
            assertTrue(payment.hasBankTransfer());
            assertFalse(payment.hasCreditCard());
            assertFalse(payment.hasCrypto());

            BankTransfer retrieved = payment.getBankTransfer();
            assertNotNull(retrieved);
            assertEquals("123456789", retrieved.getAccountNumber());
            assertEquals("SWIFT123", retrieved.getBankCode());
        }

        @Test
        @DisplayName("Payment without method - v1")
        void paymentWithoutMethodV1() {
            VersionContext ctx = VersionContext.forVersion(1);
            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-003")
                    .setAmount(5000L)
                    .build();

            assertEquals(Payment.MethodCase.METHOD_NOT_SET, payment.getMethodCase());
            assertFalse(payment.hasCreditCard());
            assertFalse(payment.hasBankTransfer());
            assertFalse(payment.hasCrypto());
        }

        @Test
        @DisplayName("Crypto not available in v1 - returns null")
        void cryptoNotAvailableInV1() {
            VersionContext ctx = VersionContext.forVersion(1);
            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-004")
                    .setAmount(1000L)
                    .build();

            // Crypto is v2-only, should not be available in v1
            assertFalse(payment.hasCrypto());
            assertNull(payment.getCrypto());
        }
    }

    @Nested
    @DisplayName("V2 Payment (with Crypto option)")
    class V2PaymentTests {

        @Test
        @DisplayName("Payment with crypto - v2")
        void paymentWithCryptoV2() {
            VersionContext ctx = VersionContext.forVersion(2);
            Crypto crypto = ctx.newCryptoBuilder()
                    .setWalletAddress("0x1234567890abcdef")
                    .setCurrency("ETH")
                    .setNetwork("mainnet")
                    .build();

            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-005")
                    .setAmount(50000L)
                    .setReference("REF-123")
                    .setCrypto(crypto)
                    .build();

            assertEquals(2, payment.getWrapperVersion());
            assertEquals("PAY-005", payment.getId());
            assertEquals("REF-123", payment.getReference());

            assertEquals(Payment.MethodCase.CRYPTO, payment.getMethodCase());
            assertTrue(payment.hasCrypto());
            assertFalse(payment.hasCreditCard());
            assertFalse(payment.hasBankTransfer());

            Crypto retrieved = payment.getCrypto();
            assertNotNull(retrieved);
            assertEquals("0x1234567890abcdef", retrieved.getWalletAddress());
            assertEquals("ETH", retrieved.getCurrency());
            assertEquals("mainnet", retrieved.getNetwork());
        }

        @Test
        @DisplayName("Credit card with CVV in v2")
        void creditCardWithCvvV2() {
            VersionContext ctx = VersionContext.forVersion(2);
            CreditCard card = ctx.newCreditCardBuilder()
                    .setCardNumber("5500000000000004")
                    .setExpiry("06/28")
                    .setHolderName("Jane Doe")
                    .setCvv("123")
                    .build();

            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-006")
                    .setAmount(15000L)
                    .setCreditCard(card)
                    .build();

            assertEquals(Payment.MethodCase.CREDIT_CARD, payment.getMethodCase());
            CreditCard retrieved = payment.getCreditCard();
            assertNotNull(retrieved);
            assertEquals("123", retrieved.getCvv());
        }
    }

    @Nested
    @DisplayName("Builder clear method")
    class BuilderClearTests {

        @Test
        @DisplayName("Clear oneof removes selected field")
        void clearOneofRemovesField() {
            VersionContext ctx = VersionContext.forVersion(1);
            CreditCard card = ctx.newCreditCardBuilder()
                    .setCardNumber("4111111111111111")
                    .setExpiry("12/25")
                    .build();

            Payment.Builder builder = ctx.newPaymentBuilder()
                    .setId("PAY-007")
                    .setAmount(1000L)
                    .setCreditCard(card);

            // Before clear
            Payment beforeClear = builder.build();
            assertEquals(Payment.MethodCase.CREDIT_CARD, beforeClear.getMethodCase());
            assertTrue(beforeClear.hasCreditCard());

            // Clear method
            builder.clearMethod();
            Payment afterClear = builder.build();

            assertEquals(Payment.MethodCase.METHOD_NOT_SET, afterClear.getMethodCase());
            assertFalse(afterClear.hasCreditCard());
            assertFalse(afterClear.hasBankTransfer());
        }

        @Test
        @DisplayName("Setting new oneof field replaces previous")
        void settingNewOneofFieldReplacesPrevious() {
            VersionContext ctx = VersionContext.forVersion(1);
            CreditCard card = ctx.newCreditCardBuilder()
                    .setCardNumber("4111111111111111")
                    .setExpiry("12/25")
                    .build();
            BankTransfer transfer = ctx.newBankTransferBuilder()
                    .setAccountNumber("123456789")
                    .setBankCode("SWIFT")
                    .build();

            Payment payment = ctx.newPaymentBuilder()
                    .setId("PAY-008")
                    .setAmount(2000L)
                    .setCreditCard(card)
                    .setBankTransfer(transfer) // This should replace credit card
                    .build();

            assertEquals(Payment.MethodCase.BANK_TRANSFER, payment.getMethodCase());
            assertTrue(payment.hasBankTransfer());
            assertFalse(payment.hasCreditCard());
        }
    }

    @Nested
    @DisplayName("Version Conversion")
    class VersionConversionTests {

        @Test
        @DisplayName("Convert v1 credit card payment to v2")
        void convertCreditCardV1ToV2() {
            VersionContext ctx1 = VersionContext.forVersion(1);
            CreditCard card = ctx1.newCreditCardBuilder()
                    .setCardNumber("4111111111111111")
                    .setExpiry("12/25")
                    .setHolderName("John Doe")
                    .build();

            Payment v1 = ctx1.newPaymentBuilder()
                    .setId("PAY-010")
                    .setAmount(10000L)
                    .setDescription("V1 payment")
                    .setCreditCard(card)
                    .build();

            // Convert to v2
            space.alnovis.protowrapper.it.model.v2.Payment v2 =
                    v1.asVersion(space.alnovis.protowrapper.it.model.v2.Payment.class);

            assertNotNull(v2);
            assertEquals(2, v2.getWrapperVersion());
            assertEquals("PAY-010", v2.getId());
            assertEquals(10000L, v2.getAmount());
            assertEquals("V1 payment", v2.getDescription());

            // Oneof should be preserved
            assertEquals(Payment.MethodCase.CREDIT_CARD, v2.getMethodCase());
            assertTrue(v2.hasCreditCard());
            assertNotNull(v2.getCreditCard());
            assertEquals("4111111111111111", v2.getCreditCard().getCardNumber());

            // V2-only fields should be default (proto2 uses empty string for unset strings)
            assertFalse(v2.hasReference());
            assertFalse(v2.getCreditCard().hasCvv());
        }

        @Test
        @DisplayName("Convert v2 crypto payment to v1 - crypto becomes not set")
        void convertCryptoV2ToV1() {
            VersionContext ctx2 = VersionContext.forVersion(2);
            Crypto crypto = ctx2.newCryptoBuilder()
                    .setWalletAddress("0xabc123")
                    .setCurrency("BTC")
                    .build();

            Payment v2 = ctx2.newPaymentBuilder()
                    .setId("PAY-011")
                    .setAmount(100000L)
                    .setCrypto(crypto)
                    .build();

            assertEquals(Payment.MethodCase.CRYPTO, v2.getMethodCase());

            // Convert to v1 - crypto doesn't exist in v1
            space.alnovis.protowrapper.it.model.v1.Payment v1 =
                    v2.asVersion(space.alnovis.protowrapper.it.model.v1.Payment.class);

            assertNotNull(v1);
            assertEquals(1, v1.getWrapperVersion());
            assertEquals("PAY-011", v1.getId());
            assertEquals(100000L, v1.getAmount());

            // Crypto is not supported in v1, so method should be not set
            // (protobuf will skip the unknown field during parsing)
            assertEquals(Payment.MethodCase.METHOD_NOT_SET, v1.getMethodCase());
            assertFalse(v1.hasCrypto());
            assertFalse(v1.hasCreditCard());
            assertFalse(v1.hasBankTransfer());
        }

        @Test
        @DisplayName("Convert v2 bank transfer to v1 - preserved")
        void convertBankTransferV2ToV1() {
            VersionContext ctx2 = VersionContext.forVersion(2);
            BankTransfer transfer = ctx2.newBankTransferBuilder()
                    .setAccountNumber("987654321")
                    .setBankCode("SWIFT456")
                    .setBankName("International Bank")
                    .build();

            Payment v2 = ctx2.newPaymentBuilder()
                    .setId("PAY-012")
                    .setAmount(75000L)
                    .setReference("REF-456")
                    .setBankTransfer(transfer)
                    .build();

            space.alnovis.protowrapper.it.model.v1.Payment v1 =
                    v2.asVersion(space.alnovis.protowrapper.it.model.v1.Payment.class);

            assertNotNull(v1);
            assertEquals(Payment.MethodCase.BANK_TRANSFER, v1.getMethodCase());
            assertTrue(v1.hasBankTransfer());
            assertEquals("987654321", v1.getBankTransfer().getAccountNumber());

            // Reference is v2-only
            assertNull(v1.getReference());
        }
    }

    @Nested
    @DisplayName("toBuilder with oneof")
    class ToBuilderTests {

        @Test
        @DisplayName("toBuilder preserves oneof selection")
        void toBuilderPreservesOneof() {
            VersionContext ctx = VersionContext.forVersion(1);
            BankTransfer transfer = ctx.newBankTransferBuilder()
                    .setAccountNumber("111222333")
                    .setBankCode("TEST")
                    .build();

            Payment original = ctx.newPaymentBuilder()
                    .setId("PAY-020")
                    .setAmount(3000L)
                    .setBankTransfer(transfer)
                    .build();

            // Use toBuilder and modify amount
            Payment modified = original.toBuilder()
                    .setAmount(5000L)
                    .build();

            assertEquals("PAY-020", modified.getId());
            assertEquals(5000L, modified.getAmount());
            assertEquals(Payment.MethodCase.BANK_TRANSFER, modified.getMethodCase());
            assertTrue(modified.hasBankTransfer());
            assertEquals("111222333", modified.getBankTransfer().getAccountNumber());
        }

        @Test
        @DisplayName("toBuilder allows changing oneof selection")
        void toBuilderAllowsChangingOneof() {
            VersionContext ctx = VersionContext.forVersion(1);
            BankTransfer transfer = ctx.newBankTransferBuilder()
                    .setAccountNumber("111222333")
                    .setBankCode("TEST")
                    .build();
            CreditCard card = ctx.newCreditCardBuilder()
                    .setCardNumber("4111111111111111")
                    .setExpiry("01/30")
                    .build();

            Payment original = ctx.newPaymentBuilder()
                    .setId("PAY-021")
                    .setAmount(3000L)
                    .setBankTransfer(transfer)
                    .build();

            assertEquals(Payment.MethodCase.BANK_TRANSFER, original.getMethodCase());

            // Use toBuilder and change to credit card
            Payment modified = original.toBuilder()
                    .setCreditCard(card)
                    .build();

            assertEquals(Payment.MethodCase.CREDIT_CARD, modified.getMethodCase());
            assertTrue(modified.hasCreditCard());
            assertFalse(modified.hasBankTransfer());
        }
    }
}
