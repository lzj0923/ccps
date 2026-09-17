package com.ccps.backend.controller;
import org.junit.jupiter.api.Test;
import com.ccps.backend.service.SignatureWhatsAppService;
import com.ccps.backend.config.AuthInterceptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignatureWhatsAppControllerTest {
 @Test void getAndPostBindRequestIdAndValidatedBody() throws Exception {
  var service=mock(SignatureWhatsAppService.class);
  Object controller=new AdminSignatureWhatsAppController(service);
  String release=System.getProperty("signature.release.classes");
  if(release!=null){
   var loader=new java.net.URLClassLoader(new java.net.URL[]{java.nio.file.Path.of(release).toUri().toURL()},getClass().getClassLoader()){
    @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
     if(name.startsWith("com.ccps.backend.controller.AdminSignatureWhatsAppController")){
      synchronized(getClassLoadingLock(name)){var loaded=findLoadedClass(name);if(loaded==null)loaded=findClass(name);if(resolve)resolveClass(loaded);return loaded;}
     }return super.loadClass(name,resolve);
    }
   };
   controller=loader.loadClass(AdminSignatureWhatsAppController.class.getName()).getConstructor(SignatureWhatsAppService.class).newInstance(service);
  }
  var mvc=MockMvcBuilders.standaloneSetup(controller).build();
  when(service.state(7L)).thenReturn(new SignatureWhatsAppService.State("+60123456789","ready",true));
  mvc.perform(get("/api/admin/e-signatures/requests/7/whatsapp")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ready"));
  when(service.send(3L,7L,"abcdefghijklmnopqrstuvwx","+60123456789")).thenReturn(new SignatureWhatsAppService.State("+60123456789","sent",true));
  mvc.perform(post("/api/admin/e-signatures/requests/7/whatsapp").requestAttr(AuthInterceptor.REQUEST_USER_ID,3L).contentType(MediaType.APPLICATION_JSON)
    .content("{\"token\":\"abcdefghijklmnopqrstuvwx\",\"recipientPhone\":\"+60123456789\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("sent"));
  verify(service).send(3L,7L,"abcdefghijklmnopqrstuvwx","+60123456789");
  mvc.perform(post("/api/admin/e-signatures/requests/7/whatsapp").requestAttr(AuthInterceptor.REQUEST_USER_ID,3L).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"\"}"))
    .andExpect(status().isBadRequest());
 }
}
