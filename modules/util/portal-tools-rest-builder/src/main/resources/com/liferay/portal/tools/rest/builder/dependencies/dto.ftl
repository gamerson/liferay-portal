package ${configYAML.apiPackagePath}.dto.${versionDirName};

<#compress>
	<#list openAPIYAML.components.schemas?keys as schemaName>
		import ${configYAML.apiPackagePath}.dto.${versionDirName}.${schemaName};
	</#list>
</#compress>

import com.liferay.petra.function.UnsafeSupplier;

import java.util.Date;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author ${configYAML.author}
 * @generated
 */
@Generated("")
@XmlRootElement(name = "${schemaName}")
public class ${schemaName} {

<#list javaTool.getJavaParameters(schema) as javaParameter>
	public ${javaParameter.parameterType} get${javaParameter.parameterName?cap_first}() {
		return _${javaParameter.parameterName};
	}

	public void set${javaParameter.parameterName?cap_first}(${javaParameter.parameterType} ${javaParameter.parameterName}) {
		_${javaParameter.parameterName} = ${javaParameter.parameterName};
	}

	public void set${javaParameter.parameterName?cap_first}(UnsafeSupplier<${javaParameter.parameterType}, Throwable> ${javaParameter.parameterName}UnsafeSupplier) {
		try {
			_${javaParameter.parameterName} = ${javaParameter.parameterName}UnsafeSupplier.get();
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private ${javaParameter.parameterType} _${javaParameter.parameterName};

</#list>
}