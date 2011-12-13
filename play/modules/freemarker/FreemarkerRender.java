package play.modules.freemarker;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.jobs.Job;
import play.libs.F.Promise;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;
import play.mvc.results.Result;
import play.vfs.VirtualFile;
import freemarker.template.Template;

/**
 * 200 OK with application/excel
 * 
 * This Result try to render Excel file with given template and beans map The
 * code use jxls and poi library to render Excel
 */
@SuppressWarnings("serial")
public class FreemarkerRender extends Result {

	public static final String RA_FILENAME = "__FILE_NAME__";
	public static final String RA_ASYNC = "__EXCEL_ASYNC__";
	public static final String CONF_ASYNC = "excel.async";

	private static VirtualFile tmplRoot = null;
	String templateName = null;
	String fileName = null; // recommended report file name
	Map<String, Object> beans = null;
	String content = null;

	private static void initTmplRoot() {
		VirtualFile appRoot = VirtualFile.open(Play.applicationPath);
		String rootDef = "";
		/*
		 * if (Play.configuration.containsKey("excel.template.root")) rootDef =
		 * (String) Play.configuration.get("excel.template.root");
		 */
		tmplRoot = appRoot.child(rootDef);
	}

	public FreemarkerRender(play.templates.Template template,
			Map<String, Object> args) {
		this.templateName = template.name;
		this.beans = args;
		if (beans != null && beans.containsKey("out")) {
			throw new RuntimeException(
					"Assertion failed! args shouldn't contain out");
		}
		// this.content = template.render(args);
	}

	public FreemarkerRender(String templateName, Map<String, Object> beans,
			String fileName) {
		this.templateName = templateName;
		this.beans = beans;
		this.fileName = fileName == null ? fileName_(templateName) : fileName;
	}

	public FreemarkerRender(String name, Map<String, Object> args) {
		this.templateName = name;
		this.beans = args;
		this.fileName = fileName == null ? fileName_(templateName) : fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public static boolean async() {
		Object o = null;
		if (RenderArgs.current().data.containsKey(RA_ASYNC)) {
			o = RenderArgs.current().get(RA_ASYNC);
		} else {
			o = Play.configuration.get(CONF_ASYNC);
		}
		boolean async = false;
		if (null == o)
			async = false;
		else if (o instanceof Boolean)
			async = (Boolean) o;
		else
			async = Boolean.parseBoolean(o.toString());
		return async;
	}

	private static String fileName_(String path) {
		if (RenderArgs.current().data.containsKey(RA_FILENAME))
			return RenderArgs.current().get(RA_FILENAME, String.class);
		int i = path.lastIndexOf("/");
		if (-1 == i)
			return path;
		return path.substring(++i);
	}

	public static void main(String[] args) {
		System.out.println(fileName_("abc.xls"));
		System.out.println(fileName_("/xyz/abc.xls"));
		System.out.println(fileName_("app/xyz/abc.xls"));
	}

	@Override
	public void apply(Request request, Response response) {
		long start = System.currentTimeMillis();
		try {
			if (null == tmplRoot) {
				initTmplRoot();
			}
			setContentTypeIfNotSet(response, "text/html; charset=UTF-8");
			Template tmp = Plugin.getTemplate(this.templateName);
			Map<String, Object> root = new HashMap<String, Object>();
			root.putAll(beans);
			Writer writer = new PrintWriter(response.out);
			tmp.process(root, writer);
			response.out.flush();

			/*
			 * InputStream is = tmplRoot.child(templateName).inputstream();
			 * Workbook workbook = new XLSTransformer() .transformXLS(is,
			 * beans); workbook.write(response.out); is.close();
			 */
			Logger.debug("Excel sync render takes %sms",
					System.currentTimeMillis() - start);
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	public void preRender() {
		try {
			if (null == tmplRoot) {
				initTmplRoot();
			}
			/*
			 * InputStream is = tmplRoot.child(templateName).inputstream();
			 * Workbook workbook = new XLSTransformer().transformXLS(is, beans);
			 * ByteArrayOutputStream os = new ByteArrayOutputStream();
			 * workbook.write(os); excel = os.toByteArray(); is.close();
			 */
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
	}

	public static Promise<FreemarkerRender> renderAsync(
			final String templateName, final Map<String, Object> beans,
			final String fileName) {
		final String fn = fileName == null ? fileName_(templateName) : fileName;
		return new Job<FreemarkerRender>() {
			@Override
			public FreemarkerRender doJobWithResult() throws Exception {
				FreemarkerRender fm = new FreemarkerRender(templateName, beans,
						fn);
				fm.preRender();
				return fm;
			}
		}.now();
	}

}
