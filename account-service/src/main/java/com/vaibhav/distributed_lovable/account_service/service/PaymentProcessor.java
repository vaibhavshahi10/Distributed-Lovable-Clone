package com.vaibhav.distributed_lovable.account_service.service;

import com.stripe.model.StripeObject;
import com.vaibhav.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.vaibhav.distributed_lovable.account_service.dto.subscription.CheckoutResponse;
import com.vaibhav.distributed_lovable.account_service.dto.subscription.PortalResponse;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
