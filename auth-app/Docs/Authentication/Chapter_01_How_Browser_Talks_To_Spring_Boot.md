# Chapter 1 - How Does a Browser Talk to Your Spring Boot Application?

## (From First Principles)

> **Goal of this Chapter**
>
> By the end of this chapter, you should understand:
>
> * Why websites need HTTP
> * Why Java programs cannot receive browser requests directly
> * What happens when you click **Send** in Postman
> * What a Request and Response are
> * Why Tomcat exists (high-level overview)
> * The complete journey before Spring Boot even starts working

---

# Before We Learn Spring Security...

Let's ask a very simple question.

Suppose you create a Java program.

```java
public class Main {

    public static void main(String[] args) {

        System.out.println("Hello Aditya");

    }

}
```

You run it.

Output

```
Hello Aditya
```

Program ends.

Question:

Can Chrome communicate with  this program?

For example,

can Chrome send

```
GET http://localhost:8080/users
```

to this Java program?

**Answer**

No.

---

# Why Not?

Think carefully.

This Java program knows only one thing.

Execute

```java
main()
```

and terminate.

It doesn't know

* HTTP
* Browser
* Internet
* URL
* Port
* Request
* Response

It simply executes Java code.

---

## Think Like This

Imagine your grandmother speaks only Hindi.

A tourist from Japan starts speaking Japanese.

Can they communicate?

No.

Not because either person is wrong.

They simply speak different languages.

Exactly the same thing happens here.

Browser speaks

```
HTTP
```

Java speaks

```
Java Objects
```

Someone must translate between them.

---

# Real Life Example

Imagine a restaurant.

Customer

↓

Waiter

↓

Chef

Customer does NOT speak directly to the chef.

Instead

Customer says

"I want a Pizza."

The waiter converts the order into something the chef understands.

The chef cooks.

The waiter brings the food back.

Browser and Java work exactly the same way.

---

# Question

Who is the Customer?

Answer

```
Browser

or

Postman
```

---

# Who is the Chef?

```
Your Spring Boot Application
```

---

# Who is the Waiter?

We will eventually learn

```
Tomcat
```

But first,

we need to understand

How does the customer even place the order?

---

# What is the Internet?

People often say

"My request goes through the Internet."

But what does that actually mean?

Very simple.

The Internet is simply

> A huge network of computers connected together.

Imagine this.

```
Computer A

──────────────

Computer B

──────────────

Computer C

──────────────

Computer D
```

Each computer can send messages to another computer.

Your laptop is one computer.

Google's servers are other computers.

Amazon's servers are other computers.

Your Spring Boot application is also running on a computer.

---

# Example

Suppose you type

```
google.com
```

Chrome sends a message.

Google receives it.

Google sends HTML back.

Chrome displays the webpage.

That's communication over the Internet.

---

# What is Communication?

Imagine you call your friend.

You say

```
Hello
```

Friend replies

```
Hi
```

That is communication.

Computers also communicate.

Instead of speaking English,

they use protocols.

---

# What is a Protocol?

Definition

> A protocol is simply a set of rules that two computers agree to follow while communicating.

Think of cricket.

Everyone follows rules.

Otherwise nobody knows

* when you're out
* when it's a six
* how many overs exist

Computers also need rules.

---

# HTTP

The protocol used by browsers and web servers is called

```
HTTP
```

HTTP means

```
HyperText Transfer Protocol
```

Don't memorize the full form.

Just remember

> HTTP is the language spoken between browsers and web servers.

---

# Example

Chrome never says

```
Give me users.
```

Instead,

it sends

```
GET /users HTTP/1.1
```

This is HTTP.

---

# What is a Request?

Definition

A request is simply

> A message sent from the client asking the server to do something.

Examples

```
GET /users
```

means

```
Please give me all users.
```

---

```
POST /login
```

means

```
Please login this user.
```

---

```
DELETE /users/5
```

means

```
Delete user whose id is 5.
```

Every request asks the server to perform some work.

---

# Who Sends Requests?

Usually

* Browser
* Mobile App
* Postman
* React Application

These are called

```
Clients
```

---

# What is a Client?

Definition

A client is

> Any application that requests a service from another application.

Examples

* Chrome
* Firefox
* Postman
* React App
* Android App

All of these are clients.

---

# What is a Server?

Definition

A server is

> A program that waits for requests and sends responses.

Your Spring Boot application is a server.

Google's backend is a server.

Netflix backend is a server.

Amazon backend is a server.

---

# Think About This

Restaurant

Customer

↓

Waiter

↓

Chef

Software World

Client

↓

Server

Same idea.

---

# What is a Response?

Definition

A response is

> The answer sent back by the server.

Example

Client

```
GET /users
```

Server replies

```json
[
    {
        "name":"Aditya"
    }
]
```

This is the response.

---

# Real Example

Open Postman.

Send

```
GET http://localhost:8080/users
```

Postman sends

```
Request
```

Spring Boot sends

```
Response
```

Exactly like asking a question and receiving an answer.

---

# What is a URL?

Example

```
http://localhost:8080/users
```

Let's break it.

```
http://
```

Protocol

```
localhost
```

Computer

```
8080
```

Port

```
/users
```

Resource

We'll study each one separately in the next chapters.

---

# Our Running Example

Throughout this entire book,

we will trace this API.

```
GET http://localhost:8080/users
```

Every chapter will move it one step further.

Current chapter

```
Client

↓

HTTP Request
```

Next chapter

```
Client

↓

Web Server

↓

Tomcat
```

Then

```
Client

↓

Tomcat

↓

Servlet
```

Eventually

```
Client

↓

Tomcat

↓

DelegatingFilterProxy

↓

FilterChainProxy

↓

SecurityFilterChain

↓

Authentication Filter

↓

Authorization Filter

↓

DispatcherServlet

↓

Controller

↓

Service

↓

Repository

↓

Database
```

---

# Key Takeaways

✔ A normal Java program cannot understand HTTP.

✔ Browsers and servers communicate using HTTP.

✔ A Client sends Requests.

✔ A Server sends Responses.

✔ Spring Boot is a server.

✔ Browsers never directly call Controllers.

✔ Something must receive HTTP requests before Spring Boot handles them.

We will discover what that "something" is in the next chapter.

---

# Interview Questions

### Q1 Why can't a normal Java program become a website?

---

### Q2 What is HTTP?

---

### Q3 What is a Client?

---

### Q4 What is a Server?

---

### Q5 What is the difference between a Request and a Response?

---

### Q6 Why can't Chrome directly call a Controller?

---

# Notebook Revision

```
Browser

↓

HTTP Request

↓

???

↓

Spring Boot

↓

HTTP Response

↓

Browser
```

In the next chapter, we'll replace the

```
???
```

with

```
Tomcat
```

and understand exactly why it exists, how it starts, how it listens on port **8080**, and how it receives requests from the browser.
