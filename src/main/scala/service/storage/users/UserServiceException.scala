package service.storage.users

class UserServiceException() extends Exception

class UserAlreadyCreatedException extends UserServiceException

class UserNotFoundException extends UserServiceException
