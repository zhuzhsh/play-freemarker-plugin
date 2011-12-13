package play.modules.freemarker;

import play.Logger;
import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.templates.BaseTemplate;
import play.templates.GroovyTemplate;
import play.templates.GroovyTemplateCompiler;
import play.templates.Template;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class FMTemplateLoader extends TemplateLoader {
	public static Template load(VirtualFile file) {
		// Try with plugin
		Template pluginProvided = Play.pluginCollection.loadTemplate(file);
		if (pluginProvided != null) {
			return pluginProvided;
		}

		// Use default engine
		final String key = getUniqueNumberForTemplateFile(file.relativePath());
		if (!templates.containsKey(key)
				|| templates.get(key).compiledTemplate == null) {
			if (Play.usePrecompiled) {
				BaseTemplate template = new GroovyTemplate(file.relativePath()
						.replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_")
						.replace("..", "parent"), file.contentAsString());
				try {
					template.loadPrecompiled();
					templates.put(key, template);
					return template;
				} catch (Exception e) {
					Logger.warn(
							"Precompiled template %s not found, trying to load it dynamically...",
							file.relativePath());
				}
			}
			BaseTemplate template = new GroovyTemplate(file.relativePath(),
					file.contentAsString());
			if (template.loadFromCache()) {
				templates.put(key, template);
			} else {
				templates.put(key, new GroovyTemplateCompiler().compile(file));
			}
		} else {
			BaseTemplate template = templates.get(key);
			if (Play.mode == Play.Mode.DEV
					&& template.timestamp < file.lastModified()) {
				templates.put(key, new GroovyTemplateCompiler().compile(file));
			}
		}
		if (templates.get(key) == null) {
			throw new TemplateNotFoundException(file.relativePath());
		}
		return templates.get(key);
	}

	/**
	 * Load a template from a String
	 * 
	 * @param key
	 *            A unique identifier for the template, used for retreiving a
	 *            cached template
	 * @param source
	 *            The template source
	 * @return A Template
	 */
	public static BaseTemplate load(String key, String source) {
		if (!templates.containsKey(key)
				|| templates.get(key).compiledTemplate == null) {
			BaseTemplate template = new GroovyTemplate(key, source);
			if (template.loadFromCache()) {
				templates.put(key, template);
			} else {
				templates.put(key,
						new GroovyTemplateCompiler().compile(template));
			}
		} else {
			BaseTemplate template = new GroovyTemplate(key, source);
			if (Play.mode == Play.Mode.DEV) {
				templates.put(key,
						new GroovyTemplateCompiler().compile(template));
			}
		}
		if (templates.get(key) == null) {
			throw new TemplateNotFoundException(key);
		}
		return templates.get(key);
	}

	public static BaseTemplate loadString(String source) {
		BaseTemplate template = new GroovyTemplate(source);
		return new GroovyTemplateCompiler().compile(template);
	}
}
