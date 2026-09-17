package com.ccps.backend.controller;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import static org.assertj.core.api.Assertions.assertThat;

class SignatureWhatsAppPathBindingTest {
 @Test void directDispatchDoesNotDependOnCompilerParameterNames(){
  for(var method:AdminSignatureWhatsAppController.class.getDeclaredMethods()){
   for(var parameter:method.getParameters()){
    var binding=parameter.getAnnotation(PathVariable.class);
    if(binding!=null)assertThat(binding.value()).as(method.getName()+" explicit path binding").isEqualTo("requestId");
   }
  }
 }
}
