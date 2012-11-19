package ca.spencerelliott.scatterfy.services;

interface MessengerCallback {
	void update(String message);
	void newMessage(String from, String message);
}