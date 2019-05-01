package ${package}.dto;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class User implements Serializable {

	private static final long serialVersionUID = 1113488483222411111L;

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}

