package service

class EventServiceException() extends Exception

class EventNotFound() extends EventServiceException

class UserAlreadyAdded() extends EventServiceException

class UserNotPresent() extends EventServiceException
