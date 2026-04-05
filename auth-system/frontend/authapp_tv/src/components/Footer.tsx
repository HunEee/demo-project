import { motion } from "framer-motion";

export default function Footer() {
  return (
    <footer className="py-10 text-center text-muted-foreground border-t border-border">
      <motion.p
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        transition={{ duration: 0.6 }}
      >
        © {new Date().getFullYear()} SecureAuth. All rights reserved.
      </motion.p>
    </footer>
  );
}