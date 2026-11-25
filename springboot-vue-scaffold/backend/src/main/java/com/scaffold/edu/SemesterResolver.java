//package com.scaffold.edu;
//
//import com.scaffold.entity.EduSemester;
//
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public final class SemesterResolver {
//
//    private static final Pattern YEAR_SPAN = Pattern.compile("(20\\d{2})\\s*[-~/到至]\\s*(20\\d{2})");
//    private static final Pattern YEAR = Pattern.compile("(20\\d{2})学年");
//
//    private SemesterResolver() {
//    }
//
//    public static EduSemester resolve(String question, List<EduSemester> all) {
//        if (question == null || all == null || all.isEmpty()) {
//            return null;
//        }
//        for (EduSemester item : all) {
//            if (item.getName() != null && question.contains(item.getName())) {
//                return item;
//            }
//            if (containsCode(question, item.getCode())) {
//                return item;
//            }
//        }
//        if (contains(question, "本学期", "这学期", "当前学期")) {
//            return all.stream()
//                    .filter(item -> item.getCurrentFlag() != null && item.getCurrentFlag() == 1)
//                    .findFirst()
//                    .orElse(null);
//        }
//        Integer term = termOf(question);
//        if (term == null) {
//            return null;
//        }
//        String year = yearOf(question);
//        EduSemester fallback = null;
//        for (EduSemester item : all) {
//            if (!termMatches(item, term)) {
//                continue;
//            }
//            if (year != null && matchesYear(item, year)) {
//                return item;
//            }
//            if (fallback == null) {
//                fallback = item;
//            }
//        }
//        return fallback;
//    }
//
//    private static Integer termOf(String question) {
//        if (contains(question, "第一学期", "上学期", "第1学期")) {
//            return 1;
//        }
//        if (contains(question, "第二学期", "下学期", "第2学期")) {
//            return 2;
//        }
//        return null;
//    }
//
//    private static boolean termMatches(EduSemester item, int term) {
//        String name = item.getName() == null ? "" : item.getName();
//        String code = item.getCode() == null ? "" : item.getCode();
//        if (term == 1) {
//            return name.contains("第一") || code.endsWith("-1") || code.endsWith("01");
//        }
//        return name.contains("第二") || code.endsWith("-2") || code.endsWith("02");
//    }
//
//    private static String yearOf(String question) {
//        Matcher span = YEAR_SPAN.matcher(question);
//        if (span.find()) {
//            return span.group(1) + "-" + span.group(2);
//        }
//        Matcher year = YEAR.matcher(question);
//        if (year.find()) {
//            return year.group(1);
//        }
//        return null;
//    }
//
//    private static boolean matchesYear(EduSemester item, String year) {
//        String name = item.getName() == null ? "" : item.getName();
//        String code = item.getCode() == null ? "" : item.getCode();
//        if (name.contains(year) || code.contains(year)) {
//            return true;
//        }
//        String start = year.length() >= 4 ? year.substring(0, 4) : year;
//        return name.contains(start) || code.startsWith(start);
//    }
//
//    private static boolean containsCode(String question, String code) {
//        if (code == null || code.isBlank()) {
//            return false;
//        }
//        int idx = question.indexOf(code);
//        while (idx >= 0) {
//            int end = idx + code.length();
//            char next = end < question.length() ? question.charAt(end) : ' ';
//            if (!Character.isDigit(next) && next != '-') {
//                return true;
//            }
//            idx = question.indexOf(code, idx + 1);
//        }
//        return false;
//    }
//
//    private static boolean contains(String text, String... words) {
//        for (String word : words) {
//            if (text.contains(word)) {
//                return true;
//            }
//        }
//        return false;
//    }
//}