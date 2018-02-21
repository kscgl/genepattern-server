package org.genepattern.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.json.JSONException;
import org.json.JSONObject;

public class ReCaptchaUtil {
    private static final Logger log = Logger.getLogger(ReCaptchaUtil.class);
    
    public static class Ex extends java.lang.Exception {
        public Ex(final String message) {
            super(message);
        }
        
        public Ex(final Throwable t) {
            super(t);
        }
        
        public Ex(final String message, final Throwable t) {
            super(message, t);
        }
    }

    public static ReCaptchaUtil init(final GpConfig gpConfig) {
        return new ReCaptchaUtil(gpConfig, GpContext.getServerContext());
    }

    public static final String PROP_ENABLED="recaptcha.enabled";
    /**
     *  'invisible' | 'v2' | 'android'
     */
    public static final String PROP_TYPE="recaptcha.type";
    public static final String PROP_SITE_KEY="recaptcha.site-key";
    public static final String PROP_SECRET_KEY="recaptcha.secret-key";
    
    public static final String PROP_VERIFY_URL="recaptcha.verify.url";
    public static final String PROP_PNAME_SECRET="recaptcha.verify.secret-key-pname";
    public static final String PROP_PNAME_RESPONSE="recaptcha.verify.response-pname";
    
    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    
    private final String verifyUrl;
    private final String secretKey;
    
    private ReCaptchaUtil(final GpConfig gpConfig, final GpContext serverContext) {
        this.secretKey=gpConfig.getGPProperty(serverContext, PROP_SECRET_KEY, "");
        this.verifyUrl=gpConfig.getGPProperty(serverContext, PROP_VERIFY_URL, "https://www.google.com/recaptcha/api/siteverify");
    }

    /**
     * Utility method to copy an InputStream into an in-memory String.
     */
    public static String copyToString(final InputStream in) throws IOException {
        StringWriter writer = new StringWriter();
        final String encoding="UTF-8";
        IOUtils.copy(in, writer, encoding);
        return writer.toString();
    }

    /**
     * Perform server side verification of the reCAPTCHA.
     * <pre>
       HTTP POST <recaptcha.verify.url>
           secret=<recaptcha.secret-key>
           response=<g-recaptcha-response>
     * </pre>
     * 
     * @param recaptchaResponseToken The value of 'g-recaptcha-response'.
     * @return true if the reCaptcha was successfully verified
     */
    public boolean verifyReCaptcha(final String recaptchaResponseToken) throws Ex {
        try {
            URL url = new URL(verifyUrl);
            StringBuilder postData = new StringBuilder();
            addParam(postData, "secret", secretKey);
            addParam(postData, "response", recaptchaResponseToken);
            final JSONObject response = postAndParseJSON(url, postData.toString());
            if (response==null) {
                log.error("Unexpected null response from 'performRecaptchaSiteVerify'");
                return false;
            }
            if (response.getBoolean("success")) {
                return true;
            }
            if (response.has("error-codes")) {
                throw new Ex("reCAPTCHA not verified: "+response.get("error-codes"));
            }
            else {
                throw new Ex("reCAPTCHA not verified: no 'error-codes' in response");
            }
        }
        catch (Ex e) {
            throw e;
        }
        catch (IOException e) {
            throw new Ex("reCAPTCHA not verified: "+e.getLocalizedMessage(), e);
        }
        catch (JSONException e) {
            throw new Ex("reCAPTCHA not verified: "+e.getLocalizedMessage(), e);
        }
        catch (Throwable t) {
            throw new Ex("reCAPTCHA not verified: "+t.getLocalizedMessage(), t);
        }
    }
    
    protected static JSONObject postAndParseJSON(URL url, String postData) throws IOException, JSONException {
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty(
            "Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty(
            "charset", StandardCharsets.UTF_8.displayName());
        urlConnection.setRequestProperty(
            "Content-Length", Integer.toString(postData.length()));
        urlConnection.setUseCaches(false);
        urlConnection.getOutputStream()
            .write(postData.getBytes(StandardCharsets.UTF_8));

        final String jsonResponse=copyToString(urlConnection.getInputStream());
        return new JSONObject(jsonResponse);
    }

    protected static StringBuilder addParam(final StringBuilder postData, final String param, final String value)
    throws UnsupportedEncodingException {
        if (postData.length() != 0) {
            postData.append("&");
        }
        return postData.append(
            String.format("%s=%s",
                URLEncoder.encode(param, StandardCharsets.UTF_8.displayName()),
                URLEncoder.encode(value, StandardCharsets.UTF_8.displayName()) 
            )
        );
    }

}
