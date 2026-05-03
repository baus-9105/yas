package com.yas.payment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.yas.payment.model.PaymentProvider;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PaymentProviderMapperTest {

    private PaymentProviderMapper paymentProviderMapper;
    private CreatePaymentProviderMapper createPaymentProviderMapper;
    private UpdatePaymentProviderMapper updatePaymentProviderMapper;

    @BeforeEach
    void setUp() {
        paymentProviderMapper = Mappers.getMapper(PaymentProviderMapper.class);
        createPaymentProviderMapper = Mappers.getMapper(CreatePaymentProviderMapper.class);
        updatePaymentProviderMapper = Mappers.getMapper(UpdatePaymentProviderMapper.class);
    }

    @Test
    void paymentProviderMapper_toVm_shouldMapCorrectly() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId("paypal");
        provider.setName("PayPal");
        provider.setConfigureUrl("http://paypal.com/config");
        provider.setVersion(1);
        provider.setMediaId(10L);

        PaymentProviderVm vm = paymentProviderMapper.toVm(provider);

        assertThat(vm).isNotNull();
        assertThat(vm.getId()).isEqualTo("paypal");
        assertThat(vm.getName()).isEqualTo("PayPal");
        assertThat(vm.getConfigureUrl()).isEqualTo("http://paypal.com/config");
        assertThat(vm.getVersion()).isEqualTo(1);
        assertThat(vm.getMediaId()).isEqualTo(10L);
    }

    @Test
    void createPaymentProviderMapper_toModel_shouldMapCorrectlyAndSetIsNew() {
        CreatePaymentVm vm = new CreatePaymentVm();
        vm.setId("cod");
        vm.setName("COD");
        vm.setConfigureUrl("http://cod.com/config");
        vm.setLandingViewComponentName("CODComponent");
        vm.setAdditionalSettings("settings");
        vm.setEnabled(true);
        vm.setMediaId(20L);

        PaymentProvider provider = createPaymentProviderMapper.toModel(vm);

        assertThat(provider).isNotNull();
        assertThat(provider.getId()).isEqualTo("cod");
        assertThat(provider.getName()).isEqualTo("COD");
        assertThat(provider.getConfigureUrl()).isEqualTo("http://cod.com/config");
        assertThat(provider.getLandingViewComponentName()).isEqualTo("CODComponent");
        assertThat(provider.getAdditionalSettings()).isEqualTo("settings");
        assertThat(provider.isEnabled()).isTrue();
        assertThat(provider.getMediaId()).isEqualTo(20L);
        assertThat(provider.isNew()).isTrue();
    }

    @Test
    void createPaymentProviderMapper_toVmResponse_shouldMapCorrectly() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId("cod");
        provider.setName("COD");
        provider.setConfigureUrl("http://cod.com/config");

        PaymentProviderVm vm = createPaymentProviderMapper.toVmResponse(provider);

        assertThat(vm).isNotNull();
        assertThat(vm.getId()).isEqualTo("cod");
        assertThat(vm.getName()).isEqualTo("COD");
        assertThat(vm.getConfigureUrl()).isEqualTo("http://cod.com/config");
    }

    @Test
    void updatePaymentProviderMapper_partialUpdate_shouldUpdateFields() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId("cod");
        provider.setName("COD");
        provider.setEnabled(false);

        UpdatePaymentVm vm = new UpdatePaymentVm();
        vm.setName("COD Updated");
        vm.setEnabled(true);
        vm.setConfigureUrl("http://cod.com/config");

        updatePaymentProviderMapper.partialUpdate(provider, vm);

        assertThat(provider.getId()).isEqualTo("cod");
        assertThat(provider.getName()).isEqualTo("COD Updated");
        assertThat(provider.isEnabled()).isTrue();
        assertThat(provider.getConfigureUrl()).isEqualTo("http://cod.com/config");
    }

    @Test
    void updatePaymentProviderMapper_toVmResponse_shouldMapCorrectly() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId("cod");
        provider.setName("COD");

        PaymentProviderVm vm = updatePaymentProviderMapper.toVmResponse(provider);

        assertThat(vm).isNotNull();
        assertThat(vm.getId()).isEqualTo("cod");
        assertThat(vm.getName()).isEqualTo("COD");
    }
}
