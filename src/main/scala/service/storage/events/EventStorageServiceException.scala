package service.storage.events

class EventServiceException() extends Exception

class EventNotFound() extends EventServiceException

class UserAlreadyAdded() extends EventServiceException

class UserNotPresent() extends EventServiceException

class EventHasOtherParticipants() extends EventServiceException
