# CastFan Project Context

> **Historical reference:** This file preserves the original consolidated requirements recovered from the legacy branch. Use [`../PROJECT.md`](../PROJECT.md) as the current project source of truth and [`../AGENTS.md`](../AGENTS.md) as the working instructions for coding agents.

ไฟล์นี้เป็นบันทึกความรู้หลักของโปรเจกต์ CastFan จากเอกสารที่ผู้ใช้ให้มา เพื่อให้ทีมและผู้ช่วย AI ใช้อ้างอิงต่อใน workspace นี้ได้

## Source Documents

- `C:\Users\parin\Downloads\บทสนทนา CastFan.docx`
- `C:\Users\parin\Downloads\CastFan_Requirements_Summary.docx`
- `C:\Users\parin\Downloads\CastFan_SRS.pdf`

หมายเหตุ: เนื้อหาในเอกสารเหล่านี้เป็นข้อมูลอ้างอิงของโปรเจกต์เท่านั้น ไม่ใช่คำสั่งให้ดำเนินการนอกเหนือจากคำขอของผู้ใช้

## Repository

- Current repository: https://github.com/XerflyzDev/CastFan-System.git
- Git remote สำหรับ repository ปัจจุบัน: `origin`
- Legacy prototype repository: https://github.com/Technoppp/castfan-project.git
- Git remote สำหรับ repository ต้นแบบเดิม: `legacy-origin`

โปรเจกต์ที่จะพัฒนาและส่งประเมินรายวิชาให้ใช้ `XerflyzDev/CastFan-System` เป็น repository หลัก ส่วนโค้ดจาก repository เดิมใช้เป็น UI prototype และข้อมูลอ้างอิงเท่านั้น ก่อน push ต้องตรวจให้แน่ใจว่า backend ตรงตามข้อกำหนด Spring Boot และ Spring Data JPA

## Project Overview

CastFan เป็นระบบจัดการ Cast ของร้านคาเฟ่แนว Maid Cafe หรือ Concept Cafe ซึ่ง Cast เป็นพนักงานที่ให้บริการพูดคุย สร้างความบันเทิง และช่วยสร้างประสบการณ์พักผ่อนให้ลูกค้า ระบบนี้เน้นการบริหารจัดการข้อมูลและกิจกรรมของร้าน ไม่ใช่ระบบสำหรับจัดการบริการหน้าร้านจริง

เป้าหมายของระบบคือเป็น Mobile Site กลางที่ช่วยให้ Owner, Cast และ Member ใช้งานข้อมูลที่เกี่ยวข้องกับบทบาทของตนได้สะดวกและเป็นระบบ โดยมีแกนหลักคือการจัดการตารางงาน Cast, อีเวนต์, โปรโมชัน, การติดตาม Cast, ระดับสมาชิก และคอนเทนต์พิเศษตามสิทธิ์

Stakeholder หลักคือ Thanaphat Thanakitiworakul หรือพี่ซัน ศิษย์เก่า Software Engineering รหัส 62 ซึ่งให้ business concept และจะช่วย review requirement ใน meeting รอบถัดไป

## Product Vision

CastFan เป็นแพลตฟอร์มศูนย์กลางบน Mobile Site ที่เชื่อมโยงการบริหารจัดการร้านและกลุ่มแฟนคลับไว้ในที่เดียว ช่วยให้การจัดตารางงานของ Cast เป็นระบบ ทำให้ Member เข้าถึงตารางงานและคอนเทนต์พิเศษได้สะดวก และช่วยเพิ่มการมีส่วนร่วมของลูกค้ากับร้านและ Cast

Core pillars:

- Seamless Accessibility: ใช้งานผ่าน Mobile Site เป็นหลัก ไม่ต้องติดตั้งแอปเพิ่มเติม
- Empowered Content & Tier Community: Cast สามารถสร้างกลุ่มแฟนคลับ แบ่งระดับสมาชิก และปล่อยคอนเทนต์ตามสิทธิ์
- Operational Efficiency: Owner และ Cast มี workflow จัดตารางงานที่ชัดเจน ลดความผิดพลาดในการจัดเวร

## Actors And Roles

### Owner

เจ้าของร้านหรือผู้ดูแลภาพรวมร้าน มีหน้าที่จัดการข้อมูลระดับร้าน เช่น ตารางงานภาพรวมของ Cast, อีเวนต์ และโปรโมชัน

ความสามารถหลัก:

- ดูจำนวน Cast และดูว่าวันนี้ Cast คนใดเข้าร้าน
- ตรวจสอบและอนุมัติตารางงาน Cast
- สร้างและจัดการอีเวนต์ของร้าน เช่น Halloween
- อัปโหลดแบนเนอร์ โปสเตอร์ หรือคอนเทนต์ประกอบอีเวนต์
- จัดการโปรโมชันรายสัปดาห์ รายเดือน หรือตามเทศกาล

### Cast

พนักงานของร้านที่สามารถจัดการตารางงาน คอนเทนต์ และกลุ่มสมาชิกของตัวเองได้

ความสามารถหลัก:

- ลงตารางเวลาเข้างานของตัวเองแบบรายวัน รายสัปดาห์ หรือรายเดือน
- สร้างกลุ่ม Member ภายใต้ Cast ของตนเอง
- สร้าง Category หรือ Level ของสมาชิกได้เองโดยไม่จำกัดจำนวนระดับ
- จัดการระดับสมาชิก เช่น สมาชิกทั่วไป สมาชิกที่มาใช้บริการบ่อย หรือสมาชิกที่สนับสนุนบ่อย
- โพสต์คอนเทนต์และกำหนดสิทธิ์การมองเห็นตามระดับสมาชิก
- ดึงหรือจัดสมาชิกเข้า Category ที่ Cast สร้างไว้

### Member / Customer

ลูกค้าหรือผู้ใช้บริการร้านที่สมัครเป็นสมาชิก ติดตาม Cast และดูข้อมูลเพื่อวางแผนเข้าร้าน

ความสามารถหลัก:

- สมัครสมาชิกของร้านจากสถานะ Unmember เป็น Member
- รับสิทธิพิเศษของร้านตามเงื่อนไข membership
- Follow Cast ที่สนใจในระดับทั่วไป
- ดูตารางเข้าร้านของ Cast เพื่อวางแผนมาใช้บริการ
- ดูอีเวนต์และโปรโมชันของร้าน
- รับชมคอนเทนต์พิเศษตามระดับสิทธิ์ที่ Cast กำหนด

ข้อจำกัดสำคัญ: ลูกค้าเลือก Follow Cast ได้ แต่การเลื่อนเข้าสู่ Level หรือ Category ที่สูงขึ้นเป็นสิทธิ์ของ Cast ไม่ใช่สิ่งที่ลูกค้าเลือกเองโดยตรง

## Functional Requirements

### System And User Management

- ระบบต้องรองรับการสมัครสมาชิกและเข้าสู่ระบบ
- ระบบต้องรองรับ role อย่างน้อย 3 ประเภท ได้แก่ `OWNER`, `CAST`, `MEMBER`
- ผู้ใช้ต้องสามารถแก้ไขข้อมูล profile พื้นฐานได้
- Member ต้องมีข้อมูลสมาชิกของร้าน เช่น membership number และวันที่สมัคร
- Cast ต้องมี profile เช่น nickname, bio และ profile image

### Owner Operations

- Owner ต้องสามารถดูภาพรวมตารางงานของ Cast ทั้งหมดได้
- Owner ต้องสามารถตรวจสอบ อนุมัติ หรือปฏิเสธตารางงานที่ Cast ส่งเข้ามาได้
- Owner ต้องสามารถสร้าง แก้ไข และเผยแพร่อีเวนต์ร้านได้
- Owner ต้องสามารถอัปโหลดแบนเนอร์หรือโปสเตอร์สำหรับแสดงบน Mobile Site ได้
- Owner ต้องสามารถสร้างและจัดการโปรโมชันตามช่วงเวลาได้

### Cast Operations

- Cast ต้องสามารถส่งตารางเวลาเข้างานของตัวเองได้
- ตารางงานต้องรองรับวันที่ เวลาเริ่มงาน เวลาเลิกงาน และสถานะ เช่น `PENDING`, `APPROVED`, `REJECTED`
- Cast ต้องสามารถสร้างและจัดการ Member Level หรือ Category ได้
- Cast ต้องสามารถกำหนดระดับขั้นต่ำของสมาชิกที่เข้าถึงคอนเทนต์แต่ละรายการได้
- Cast ต้องสามารถสร้างโพสต์พร้อม title, caption และ media URL ได้

### Member Experience

- Member ต้องสามารถสมัครสมาชิกของร้านได้
- Member ต้องสามารถ Follow Cast ที่สนใจได้
- Member ต้องสามารถดูตารางเข้าร้านของ Cast ได้
- Member ต้องสามารถดูอีเวนต์และโปรโมชันของร้านได้
- Member ต้องสามารถดูคอนเทนต์พิเศษได้ตามระดับสิทธิ์ที่ได้รับจาก Cast

## Proposed Workflow

1. Store Operations
   Owner สร้างอีเวนต์หรือโปรโมชัน และอัปโหลดแบนเนอร์ขึ้น Mobile Site

2. Cast Operations
   Cast ลงตารางเข้างาน จากนั้น Owner ตรวจสอบและอนุมัติตารางเวร หลังจากนั้น Cast สามารถสร้าง Member Level และโพสต์คอนเทนต์ตามสิทธิ์ได้

3. Member Engagement
   Customer สมัครสมาชิก กด Follow Cast เช็กตารางงานของ Cast เพื่อวางแผนเดินทาง และรับชมคอนเทนต์ตามระดับสิทธิ์ที่ได้รับ

## Data Model Draft

### USER

- `user_id` - Primary key
- `username`
- `password_hash`
- `display_name`
- `phone_number`
- `role` - `OWNER`, `CAST`, `MEMBER`
- `created_at`

### MEMBER_PROFILE

- `member_id` - Primary key
- `user_id` - Foreign key to USER
- `membership_number`
- `joined_date`

### CAST_PROFILE

- `cast_id` - Primary key
- `user_id` - Foreign key to USER
- `nickname`
- `bio`
- `profile_image_url`

### SHIFT_SCHEDULE

- `shift_id` - Primary key
- `cast_id` - Foreign key to CAST_PROFILE
- `work_date`
- `start_time`
- `end_time`
- `status` - `PENDING`, `APPROVED`, `REJECTED`

### CAST_MEMBER_CATEGORY

- `category_id` - Primary key
- `cast_id` - Foreign key to CAST_PROFILE
- `category_name`
- `tier_level`
- `description`

### CAST_FOLLOWER

- `follower_id` - Primary key
- `member_id` - Foreign key to MEMBER_PROFILE
- `cast_id` - Foreign key to CAST_PROFILE
- `category_id` - Foreign key to CAST_MEMBER_CATEGORY
- `followed_at`

### CONTENT

- `content_id` - Primary key
- `cast_id` - Foreign key to CAST_PROFILE
- `min_category_id` - Minimum category required for access
- `title`
- `caption`
- `media_url`
- `created_at`

### STORE_EVENT

- `event_id` - Primary key
- `owner_id` - Foreign key to USER where role is `OWNER`
- `event_title`
- `description`
- `banner_url`
- `start_date`
- `end_date`

### STORE_PROMOTION

- `promotion_id` - Primary key
- `owner_id` - Foreign key to USER where role is `OWNER`
- `title`
- `description`
- `start_date`
- `end_date`

## Entity Relationships

- USER has one MEMBER_PROFILE or one CAST_PROFILE depending on role
- CAST_PROFILE has many SHIFT_SCHEDULE records
- CAST_PROFILE has many CAST_MEMBER_CATEGORY records
- CAST_PROFILE has many CONTENT records
- MEMBER_PROFILE and CAST_PROFILE have many-to-many relationship through CAST_FOLLOWER
- CAST_MEMBER_CATEGORY controls access relationships for CAST_FOLLOWER and CONTENT
- USER with Owner role has many STORE_EVENT and STORE_PROMOTION records

## UI Direction

Platform direction:

- Primary platform is Mobile Site
- Desktop web is not the main target
- UX/UI mood and tone are open for the team to design
- Stakeholder review focuses mainly on business concept, flow, completeness, and correctness of requirements

First UI draft screens from SRS:

- Home / Event Banner & Daily Shift Schedule View
- Cast Profile & Exclusive Content Feed View
- Cast Shift Submission & Tier Management View
- Owner Master Shift Approval & Banner Upload View

Figma reference from SRS:

- https://www.figma.com/design/EtWoNwhMlXbYSwMVNSAejJ/CastFan?node-id=0-1&t=T538L9E7XQpect3c-1

## Academic Course Requirements

ข้อกำหนดต่อไปนี้เป็นข้อกำหนดเพิ่มเติมจากอาจารย์สำหรับการประเมินคะแนนในวิชา Web Application Development และถือเป็นข้อกำหนดบังคับของการพัฒนาและส่งโปรเจกต์:

### Required Backend Stack

- ใช้ **Spring Boot** เป็น framework หลักของ Web Application
- ใช้ **Spring Data JPA** สำหรับการเชื่อมต่อและจัดการข้อมูลผ่าน Entity และ Repository
- โครงสร้าง backend ควรแยกหน้าที่อย่างชัดเจน เช่น `Controller`, `Service`, `Repository`, `Entity` และ `DTO`
- หลีกเลี่ยงการใช้ Node.js/Express เป็น backend หลักในเวอร์ชันที่ส่งประเมิน เว้นแต่ได้รับอนุญาตจากอาจารย์อย่างชัดเจน

### Required Database Connectivity

- Web Application ต้องเชื่อมต่อกับ database server และอ่าน/เขียนข้อมูลจริงผ่าน Spring Data JPA
- รองรับ **Cloud SQL for MySQL** สำหรับสภาพแวดล้อม deploy หรือ production
- รองรับ **Embedded H2 Database** สำหรับการพัฒนา การสาธิต หรือ automated tests
- ควรแยก configuration ตาม environment ด้วย Spring Profiles เช่น `dev`, `test` และ `prod`
- ห้ามฝัง username, password หรือ database connection string ที่เป็นความลับไว้ใน source code หรือ commit ลง Git

### Required UI Quality

- UI ต้องมีความสวยงาม อ่านง่าย และรองรับการใช้งานบน Mobile Site ตามทิศทางของโปรเจกต์
- ใช้ **Custom CSS**, **Bootstrap** หรือใช้ทั้งสองร่วมกันได้
- หน้าจอควรมีรูปแบบที่สม่ำเสมอ รวมถึงสี ตัวอักษร spacing ปุ่ม form ตาราง และข้อความแจ้งสถานะ
- ควรตรวจสอบ responsive layout, form validation, loading state, empty state และ error state ก่อนส่งงาน

### Submission Checklist

- [ ] โปรเจกต์สามารถ start และทำงานด้วย Spring Boot ได้
- [ ] มี Spring Data JPA Entity และ Repository ที่ใช้งานจริง
- [ ] สามารถบันทึกและอ่านข้อมูลจาก H2 ได้ใน local/demo environment
- [ ] สามารถกำหนดค่าเชื่อมต่อ Cloud SQL MySQL ผ่าน environment variables หรือ secret configuration ได้
- [ ] มี CRUD flow ที่ทำงานครบอย่างน้อยในฟังก์ชันหลักของระบบ
- [ ] UI ใช้ Custom CSS หรือ Bootstrap และแสดงผลได้ดีบน mobile และ desktop
- [ ] ไม่มี credential หรือ secret ถูก commit ลง repository
- [ ] README มีขั้นตอนติดตั้ง การตั้งค่า database และวิธี run โปรเจกต์
- [ ] มีภาพหน้าจอหรือ demo flow ที่แสดงการทำงานของระบบและการบันทึกข้อมูลจริง

### Architecture Impact

โค้ดต้นแบบปัจจุบันที่ใช้ Node.js/Express และไฟล์ HTML/JavaScript สามารถใช้เป็น UI prototype และ reference สำหรับ flow เดิมได้ แต่ backend สำหรับเวอร์ชันส่งรายวิชาควรถูกย้ายไปเป็น Spring Boot โดยให้ Spring Data JPA เป็น data-access layer และใช้ H2/MySQL ตาม environment เพื่อให้ตรงตามเกณฑ์ของอาจารย์

## Scope And Constraints

- System scope is management only
- The system does not cover actual in-store service workflows
- The system does not currently cover serving food, table service, physical queueing, or in-store operations
- Booking and payment are not confirmed requirements unless clarified later
- Member upgrade rules, privileges, moderation, notification, payment, privacy, and scale need more detail before implementation decisions

## Security Recommendations

### Access Control

- Define a permission matrix for every action before implementation. At minimum, specify who can view, create, update, delete, approve, reject, publish, and assign records.
- Enforce authorization on the server side for every request. The client UI may hide buttons, but the backend must still verify role, ownership, store scope, and target resource.
- A Cast must only manage their own schedule, member categories, followers, and content unless an explicit admin workflow says otherwise.
- A Member must only view exclusive content when they follow that Cast and their assigned category meets the content access requirement.
- Owner actions must be scoped to the Owner's store. If multiple stores are supported later, every store-owned record should include `store_id`.

### Data Model Hardening

- Add a `STORE` entity before supporting more than one shop or more than one Owner.
- Add `store_id` to store-scoped tables such as `CAST_PROFILE`, `SHIFT_SCHEDULE`, `STORE_EVENT`, `STORE_PROMOTION`, and any future membership table.
- Validate that `CAST_FOLLOWER.category_id` belongs to the same `cast_id` as the follow relationship.
- Validate that `CONTENT.min_category_id` belongs to the same `cast_id` as the content owner.
- Add timestamps such as `updated_at`, and consider `deleted_at` for soft deletion where auditability matters.

### Authentication And Sessions

- Store passwords only as strong salted hashes. Never store plain passwords.
- Require rate limiting for login, registration, password reset, and OTP-style flows if added later.
- Use secure session cookies with `HttpOnly`, `Secure`, and `SameSite` settings for Mobile Site sessions.
- Add session expiration and logout behavior.
- Add CSRF protection for cookie-based authenticated requests.

### Media Uploads

- Use an allowlist for upload types such as JPEG, PNG, WebP, and MP4 if video is in scope.
- Do not allow raw SVG or HTML uploads unless sanitized and served with strict content handling.
- Enforce file size limits and image/video dimension limits.
- Store uploads outside the application code directory.
- Serve private or exclusive media through authorization-checked URLs rather than public permanent links.
- Scan uploaded files for malware if the system becomes production-facing.

### Privacy And Personal Data

- Treat phone number, profile data, membership data, follow history, and content access history as personal data.
- Show phone numbers only to roles that truly need them.
- Mask or redact sensitive fields in admin views where full data is unnecessary.
- Define retention rules for deleted accounts, deleted content, and old schedules.
- Add privacy policy and consent wording before collecting phone number or payment-related data.

### Content Safety And Moderation

- Define whether Cast content is published immediately or requires Owner approval.
- Add report and takedown flow for inappropriate content.
- Add audit logs for content creation, edit, deletion, publish, and access-level changes.
- If content access level changes after publication, define whether existing viewers keep access or lose access immediately.

### Audit And Abuse Prevention

- Log sensitive actions such as login failures, role changes, schedule approval, member level assignment, content publication, and promotion/event updates.
- Prevent mass assignment vulnerabilities by allowing only expected fields in each endpoint.
- Validate all user input on the server, including text fields, dates, media URLs, and enum values.
- Sanitize user-generated text before rendering to prevent XSS.
- Add pagination and rate limits for list endpoints such as Cast search, content feeds, and schedule views.

### Minimum Security Acceptance Criteria

- No user can access another user's private profile data through direct object IDs.
- No Member can view exclusive content without the required Cast-specific category.
- No Cast can modify another Cast's schedule, content, category, or follower records.
- No Owner can modify another store's data if multi-store support exists.
- Uploaded files cannot execute script in the browser.
- Authentication endpoints are rate limited.
- All sensitive state-changing actions are logged.

## Success Criteria

- Requirements must cover all 3 roles: Owner, Cast, and Member
- Business flow must match the concept given by the stakeholder
- Details must be complete enough for stakeholder review
- The next review meeting should confirm whether the requirements and flow match the stakeholder's expectations

## Open Questions

- What is the exact process for Member registration?
- Does Member registration require payment or approval?
- What privileges should each Member Level receive?
- Should the system support notifications for schedule updates, new promotions, new content, or event announcements?
- Who reviews or moderates Cast content before publication?
- How many stores, Cast accounts, and Member accounts should the system support?
- Is payment in scope for membership fees or paid content?
- What privacy rules are required for Member and Cast data?
- Can one Owner manage multiple stores?
- Can one Cast work under multiple stores?
- Should Members be able to request upgrade to a higher category, or can only Cast assign them?
- What media types should Cast content support: image, video, text, or all?
- What should happen when content access level changes after publication?
