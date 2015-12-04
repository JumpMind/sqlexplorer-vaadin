package org.jumpmind.vaadin.ui.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.DefaultConverterFactory;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.data.util.converter.StringToBooleanConverter;
import com.vaadin.data.util.converter.StringToDateConverter;
import com.vaadin.data.util.converter.StringToDoubleConverter;
import com.vaadin.data.util.converter.StringToFloatConverter;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.util.converter.StringToLongConverter;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

abstract public class AbstractSpringUI extends UI {

    private static final long serialVersionUID = 1L;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    protected void init(VaadinRequest request) {
        setErrorHandler(new DefaultErrorHandler() {

            private static final long serialVersionUID = 1L;

            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                Throwable ex = event.getThrowable();
                CommonUiUtils.notify(ex);
                if (ex != null) {
                    log.error(ex.getMessage(), ex);
                } else {
                    log.error("An unexpected error occurred");
                }
            }
        });
        
        VaadinSession.getCurrent().setConverterFactory(new DefaultConverterFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Converter<Date, ?> createDateConverter(Class<?> sourceType) {
                return super.createDateConverter(sourceType);
            }
            
            protected Converter<String, ?> createStringConverter(Class<?> sourceType) {
                if (Double.class.isAssignableFrom(sourceType)) {
                    return new StringToDoubleConverter();
                } else if (Float.class.isAssignableFrom(sourceType)) {
                    return new StringToFloatConverter();
                } else if (Integer.class.isAssignableFrom(sourceType)) {
                    return new StringToIntegerConverter() {
                      private static final long serialVersionUID = 1L;
                    @Override
                        protected NumberFormat getFormat(Locale locale) {
                            NumberFormat format = super.getFormat(locale);
                            format.setGroupingUsed(false);
                            return format;
                        }  
                    };
                } else if (Long.class.isAssignableFrom(sourceType)) {
                    return new StringToLongConverter() {
                        private static final long serialVersionUID = 1L;
                      @Override
                          protected NumberFormat getFormat(Locale locale) {
                              NumberFormat format = super.getFormat(locale);
                              format.setGroupingUsed(false);
                              return format;
                          }  
                      };
                } else if (BigDecimal.class.isAssignableFrom(sourceType)) {
                    return new StringToBigDecimalConverter();
                } else if (Boolean.class.isAssignableFrom(sourceType)) {
                    return new StringToBooleanConverter();
                } else if (Date.class.isAssignableFrom(sourceType)) {
                    return new StringToDateConverter();
                } else {
                    return null;
                }
            }

            
        });        

        Responsive.makeResponsive(this);
    }

    public WebApplicationContext getWebApplicationContext() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(VaadinServlet
                .getCurrent().getServletContext());
    }
}
