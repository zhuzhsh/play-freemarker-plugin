package play.modules.freemarker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.validation.Validation;
import play.exceptions.PlayException;
import play.exceptions.TemplateNotFoundException;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Scope;
import play.templates.Template;

public class FMController extends Controller {
	 /**
     * Work out the default template to load for the invoked action.
     * E.g. "controllers.Pages.index" returns "views/Pages/index.html".
     */
    protected static String template() {
        final Request theRequest = Request.current();
        //final String format = theRequest.format;
        String templateName = theRequest.action.replace(".", "/") + ".ftl";
        /*if (templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if (!templateName.contains(".")) {
                templateName = theRequest.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "ftl" : format);
        }*/
        return templateName;
    }

    /**
     * Work out the default template to load for the action.
     * E.g. "controllers.Pages.index" returns "views/Pages/index.html".
     */
    protected static String template(String templateName) {
        //final Request theRequest = Request.current();
        //final String format = theRequest.format;
       /* if (templateName.startsWith("@")) {
            templateName = templateName.substring(1);
            if (!templateName.contains(".")) {
                templateName = theRequest.controller + "." + templateName;
            }
            templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
        }*/
        return templateName;
    }
    
    protected static void render(Object... args) {
        String templateName = null;
        if (args.length > 0 && args[0] instanceof String && LocalVariablesNamesTracer.getAllLocalVariableNames(args[0]).isEmpty()) {
            templateName = args[0].toString();
        } else {
            templateName = template();
        }
        renderTemplate(templateName, args);
    }
    
    /**
     * Render a specific template
     * @param templateName The template name
     * @param args The template data
     */
    protected static void renderTemplate(String templateName, Object... args) {
        // Template datas
        Map<String, Object> templateBinding = new HashMap<String, Object>(16);
        for (Object o : args) {
            List<String> names = LocalVariablesNamesTracer.getAllLocalVariableNames(o);
            for (String name : names) {
                templateBinding.put(name, o);
            }
        }
        renderTemplate(templateName, templateBinding);
    }
    
    protected static void renderTemplate(String templateName, Map<String,Object> args) {
        // Template datas
        Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
        templateBinding.data.putAll(args);
        templateBinding.put("session", Scope.Session.current());
        templateBinding.put("request", Http.Request.current());
        templateBinding.put("flash", Scope.Flash.current());
        templateBinding.put("params", Scope.Params.current());
        templateBinding.put("errors", Validation.errors());
        try {
            Template template = FMTemplateLoader.load(template(templateName));
            throw new FreemarkerRender(template, templateBinding.data);
        } catch (TemplateNotFoundException ex) {
            if (ex.isSourceAvailable()) {
                throw ex;
            }
            StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
            if (element != null) {
                throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
            } else {
                throw ex;
            }
        }
    }
    /**
     * Render the template corresponding to the action's package-class-method name (@see <code>template()</code>).
     *
     * @param args The template data.
     */
    protected static void renderTemplate(Map<String,Object> args) {
        renderTemplate(template(), args);
    }
    
   
}
