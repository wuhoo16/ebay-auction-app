/*  Item.java (in the ClientProject)
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */

package final_exam_pkg;

import java.math.BigDecimal;

class Item {
	protected String name;
	protected String description;
	protected double minPrice;
	protected double currentBidPrice;
	protected double buyNowPrice;
	protected String highestBidderUsername;
	protected BigDecimal duration;
	protected String soldMessage;
	
	// parameterized constructor for Client class to call - Items should only be instiantiated by calling this constructor 
	protected Item (String name, String description, double minPrice, double currentBidPrice, double buyNowPrice, String highestBidderUsername, BigDecimal duration, String soldMsg) {
		this.name = name;
		this.description = description;
		this.minPrice = minPrice;
		this.currentBidPrice = currentBidPrice;
		this.buyNowPrice = buyNowPrice;
		this.highestBidderUsername = new String(); this.highestBidderUsername += highestBidderUsername;
		this.duration = duration;
		this.soldMessage = soldMsg;
	}
	
	// setters and getters below
	protected String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected String getDescription() {
		return description;
	}

	protected void setDescription(String description) {
		this.description = description;
	}

	protected double getMinPrice() {
		return minPrice;
	}

	protected void setMinPrice(double minPrice) {
		this.minPrice = minPrice;
	}

	protected double getCurrentBidPrice() {
		return currentBidPrice;
	}

	protected void setCurrentBidPrice(double currentBidPrice) {
		this.currentBidPrice = currentBidPrice;
	}

	protected double getBuyNowPrice() {
		return buyNowPrice;
	}

	protected void setBuyNowPrice(double buyNowPrice) {
		this.buyNowPrice = buyNowPrice;
	}

	protected String getHighestBidderUsername() {
		return highestBidderUsername;
	}

	protected void setHighestBidderUsername(String highestBidderUsername) {
		this.highestBidderUsername = highestBidderUsername;
	}

	protected BigDecimal getDuration() {
		return duration;
	}

	protected void setDuration(BigDecimal duration) {
		this.duration = duration;
	}

	protected String getSoldMessage() {
		return soldMessage;
	}

	protected void setSoldMessage(String soldMessage) {
		this.soldMessage = soldMessage;
	}
}
