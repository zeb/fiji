package fiji.recorder.template;

import fiji.recorder.RecorderBase;

public class Template {
	
	private String name;
	private String description;
	private String python_string;
	
	
	/*
	 * METHODS
	 */
	
	public String toLanguage(RecorderBase.Language lang) {
		String result = null;
		switch (lang) {
		case Python:
			result = toPython();
			break;
		}
		return result;
	}
	
	/**
	 * Returns the name of this template.
	 * @return  the template name
	 */
	public String toString() {
		return name;
	}
	
	/*
	 * GETTERS AND SETTERS
	 */
	
	/**
	 * Returns the name (as a short description) of this template.
	 * @return  the template name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns a description of this template.
	 * @return  the template description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns a Python translation of this template.
	 * @return  the python string
	 */
	public String toPython() {
		return python_string;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPythonString(String pythonString) {
		python_string = pythonString;
	}


}
