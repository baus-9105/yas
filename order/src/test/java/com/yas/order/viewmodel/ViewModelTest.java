package com.yas.order.viewmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ViewModelTest {

    @Test
    @DisplayName("ErrorVm should create correctly")
    void errorVm_shouldCreateCorrectly() {
        ErrorVm vm = new ErrorVm("404", "Not Found", "Detail error");
        assertThat(vm.statusCode()).isEqualTo("404");
        assertThat(vm.title()).isEqualTo("Not Found");
        assertThat(vm.detail()).isEqualTo("Detail error");
        assertThat(vm.fieldErrors()).isEmpty();

        List<String> fieldErrors = List.of("Field 1 invalid");
        ErrorVm vm2 = new ErrorVm("400", "Bad Request", "Detail error", fieldErrors);
        assertThat(vm2.fieldErrors()).containsExactlyElementsOf(fieldErrors);
    }

    @Test
    @DisplayName("ResponeStatusVm should create correctly")
    void responseStatusVm_shouldCreateCorrectly() {
        ResponeStatusVm vm = new ResponeStatusVm("Success", "Operation successful", "200");
        assertThat(vm.statusCode()).isEqualTo("200");
        assertThat(vm.message()).isEqualTo("Operation successful");
        assertThat(vm.title()).isEqualTo("Success");
    }
}
