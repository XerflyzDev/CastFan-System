# CastFan System

CastFan System คือ Web Application สำหรับบริหารจัดการร้าน Maid Cafe หรือ Concept Cafe และเชื่อมต่อการใช้งานระหว่างเจ้าของร้าน (`Owner`), พนักงาน (`Cast`) และลูกค้าสมาชิก (`Member`) ภายในระบบเดียว

โปรเจกต์นี้จัดทำสำหรับรายวิชา **Web Application Development** และเริ่มพัฒนาใหม่ตั้งแต่ต้น โดยใช้ Spring Boot, Spring Data JPA และฐานข้อมูล H2/MySQL ตามข้อกำหนดของรายวิชา

## Project Status

> **Planning and initial development**

ขณะนี้โปรเจกต์อยู่ในช่วงเริ่มต้น กำลังออกแบบ architecture, database schema, user flow และขอบเขตของ Minimum Viable Product (MVP)

## Objectives

- ลดความซับซ้อนในการจัดและอนุมัติตารางงานของ Cast
- ช่วยให้ Member ตรวจสอบตารางและติดตาม Cast ที่สนใจได้สะดวก
- สนับสนุนการจัดการกิจกรรม โปรโมชัน และข้อมูลภายในร้าน
- จัดการระดับสมาชิกและสิทธิ์เข้าถึงคอนเทนต์ของแต่ละ Cast
- พัฒนา Mobile-first UI ที่สวยงาม ใช้งานง่าย และ responsive

## User Roles

### Member

- สมัครสมาชิกและเข้าสู่ระบบ
- ดูรายชื่อและโปรไฟล์ Cast
- ติดตาม Cast ที่สนใจ
- ดูตารางเข้างานของ Cast
- ดูกิจกรรมและโปรโมชันของร้าน
- เข้าถึงคอนเทนต์ตามระดับสมาชิก

### Cast

- จัดการโปรไฟล์ของตนเอง
- ส่งและแก้ไขตารางเวลาเข้างาน
- สร้างระดับสมาชิกสำหรับผู้ติดตาม
- จัดการสมาชิกในแต่ละระดับ
- สร้างคอนเทนต์และกำหนดสิทธิ์การเข้าถึง

### Owner

- ดูภาพรวม Cast และตารางงานของร้าน
- อนุมัติหรือปฏิเสธตารางงาน
- สร้างและจัดการกิจกรรม
- สร้างและจัดการโปรโมชัน
- จัดการแบนเนอร์และข้อมูลที่แสดงบนหน้าเว็บไซต์

## Technology Stack

| Layer | Technology |
| --- | --- |
| Backend | Java, Spring Boot |
| Web | Spring MVC |
| Persistence | Spring Data JPA |
| Development Database | Embedded H2 Database |
| Deployment Database | Cloud SQL for MySQL |
| Frontend | HTML, JavaScript, Bootstrap, Custom CSS |
| Build Tool | Maven |
| Testing | JUnit, Spring Boot Test |

## Planned Architecture

```text
Client / Mobile Browser
          |
          v
Spring MVC Controller
          |
          v
Service Layer
          |
          v
Spring Data JPA Repository
          |
          +-- H2 Database (development and test)
          |
          `-- Cloud SQL MySQL (deployment)
```

Backend จะจัดโครงสร้างตามหน้าที่ ได้แก่ `Controller`, `Service`, `Repository`, `Entity`, `DTO`, configuration และ security เพื่อให้ดูแลรักษาและทดสอบระบบได้ง่าย

## Database Environments

- `dev` - ใช้ Embedded H2 สำหรับการพัฒนาในเครื่อง
- `test` - ใช้ H2 แบบ isolated สำหรับ automated tests
- `prod` - ใช้ Cloud SQL for MySQL และรับค่าการเชื่อมต่อผ่าน environment variables หรือ secret manager

ข้อมูลสำคัญ เช่น database password, API secret และ service-account key ต้องไม่ถูก commit ลง repository

## Core Data Model

Entity หลักที่วางแผนไว้:

- `User`
- `MemberProfile`
- `CastProfile`
- `ShiftSchedule`
- `MemberTier`
- `CastFollower`
- `Content`
- `StoreEvent`
- `StorePromotion`

รายละเอียดของ Entity และความสัมพันธ์จะถูกปรับปรุงหลังจากสรุป requirements และออกแบบ ER Diagram

## Development Roadmap

- [ ] สรุป functional requirements และ acceptance criteria
- [ ] ออกแบบ wireframe และ user flow
- [ ] ออกแบบ ER Diagram และ database schema
- [ ] สร้าง Spring Boot project
- [ ] ตั้งค่า Spring Data JPA และ H2
- [ ] พัฒนา authentication และ role-based authorization
- [ ] พัฒนา workflow ของ Member
- [ ] พัฒนา workflow ของ Cast
- [ ] พัฒนา workflow ของ Owner
- [ ] เชื่อมต่อ Cloud SQL for MySQL
- [ ] เพิ่ม validation, error handling และ automated tests
- [ ] ตรวจ security และ responsive UI
- [ ] เตรียม deployment และสาธิตระบบ

## Security Requirements

- ตรวจสอบ authentication และ authorization ที่ backend ทุก endpoint
- จำกัดข้อมูลและ action ตาม role และ resource ownership
- Hash password ด้วยวิธีที่ปลอดภัย
- Validate input ทุกจุดก่อนบันทึกลงฐานข้อมูล
- ป้องกัน IDOR, XSS, CSRF, SQL injection และ brute-force login
- ไม่เก็บ credential หรือ secret ไว้ใน source code
- ตรวจสอบชนิดและขนาดของไฟล์ที่ผู้ใช้อัปโหลด
- บันทึก audit log สำหรับ action สำคัญ

## Getting Started

โปรเจกต์ยังอยู่ในช่วงเริ่มต้น ขั้นตอนติดตั้งและคำสั่ง run จะเพิ่มหลังจากสร้าง Spring Boot project structure แล้ว

## Repository

[https://github.com/XerflyzDev/CastFan-System](https://github.com/XerflyzDev/CastFan-System)

## Academic Context

โปรเจกต์นี้จัดทำเพื่อการศึกษาในรายวิชา **Web Application Development** โดยเน้นการประยุกต์ใช้ Spring Boot, Spring Data JPA, database connectivity และการออกแบบ UI ด้วย Bootstrap หรือ Custom CSS
