/**
 * Oneof field generation support.
 *
 * <p>This package provides consolidated generation of all oneof-related code,
 * including case enums, accessor methods, and builder clear operations.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link io.alnovis.protowrapper.generator.oneof.OneofGenerator} -
 *       Generates all oneof-related code for interfaces, abstract classes, and implementations</li>
 * </ul>
 *
 * <h2>Generated Code for Oneof</h2>
 * <p>For a proto oneof like:</p>
 * <pre>
 * message Payment {
 *     oneof method {
 *         CreditCard credit_card = 1;
 *         BankTransfer bank_transfer = 2;
 *     }
 * }
 * </pre>
 *
 * <p>The following code is generated:</p>
 *
 * <h3>Interface Level</h3>
 * <pre>{@code
 * public interface PaymentWrapper {
 *     enum MethodCase {
 *         CREDIT_CARD(1),
 *         BANK_TRANSFER(2),
 *         METHOD_NOT_SET(0);
 *         // ...
 *     }
 *
 *     MethodCase getMethodCase();
 *     boolean hasCreditCard();
 *     boolean hasBankTransfer();
 * }
 * }</pre>
 *
 * <h3>Abstract Class Level</h3>
 * <pre>{@code
 * public abstract class AbstractPaymentWrapper {
 *     protected abstract MethodCase extractMethodCase(T proto);
 *
 *     public MethodCase getMethodCase() {
 *         return extractMethodCase(proto);
 *     }
 *
 *     protected abstract void doClearMethod();
 *
 *     public Builder clearMethod() {
 *         doClearMethod();
 *         return this;
 *     }
 * }
 * }</pre>
 *
 * <h3>Impl Class Level</h3>
 * <pre>{@code
 * public class PaymentWrapperV1Impl {
 *     protected MethodCase extractMethodCase(Payment proto) {
 *         return MethodCase.forNumber(proto.getMethodCase().getNumber());
 *     }
 *
 *     protected void doClearMethod() {
 *         protoBuilder.clearMethod();
 *     }
 * }
 * }</pre>
 *
 * @see io.alnovis.protowrapper.model.MergedOneof
 * @see io.alnovis.protowrapper.generator.InterfaceGenerator
 */
package io.alnovis.protowrapper.generator.oneof;
