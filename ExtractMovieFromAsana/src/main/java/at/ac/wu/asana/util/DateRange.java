package at.ac.wu.asana.util;

import java.time.LocalDate;

public class DateRange {
	public LocalDate startDate;
	public LocalDate endDate;
	
	public DateRange(LocalDate start, LocalDate end) {
		startDate=start;
		endDate=end;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	@Override
	public String toString() {
		return "DateRange [startDate=" + startDate + ", endDate=" + endDate + "]";
	}
}
