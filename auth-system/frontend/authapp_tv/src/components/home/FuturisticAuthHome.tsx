import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "framer-motion";
import { Shield, Lock, Sparkles, Fingerprint } from "lucide-react";

export default function FuturisticAuthHome() {
  return (
    <div className="min-h-screen bg-background text-foreground transition-colors overflow-hidden">
      {/* Hero Section */}
      <section className="relative py-28 px-6 text-center flex flex-col items-center justify-center">
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="text-5xl md:text-7xl font-bold tracking-tight"
        >
          안전하고 빠른 미래형 인증
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.8 }}
          className="mt-6 max-w-2xl text-lg md:text-xl text-muted-foreground"
        >
          현대적인 애플리케이션을 위한 차세대 인증 플랫폼입니다.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5, duration: 0.8 }}
          className="mt-10 flex gap-4"
        >
          <Button size="lg" className="rounded-2xl text-lg px-6">
            시작하기
          </Button>
          <Button
            size="lg"
            variant="outline"
            className="rounded-2xl text-lg px-6 border-border"
          >
            자세히 보기
          </Button>
        </motion.div>
      </section>

      {/* Features Section */}
      <section className="py-24 px-6">
        <h2 className="text-4xl font-bold text-center mb-16">
          주요 기능
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto">
          {[
            {
              title: "생체 인증 로그인",
              desc: "지문 및 얼굴 인식을 활용한 차세대 보안 인증.",
              icon: <Fingerprint className="w-10 h-10" />,
            },
            {
              title: "다중 암호화",
              desc: "완벽한 보안을 위한 산업 수준의 암호화 기술.",
              icon: <Lock className="w-10 h-10" />,
            },
            {
              title: "지능형 접근 제어",
              desc: "실시간 위협에 대응하는 AI 기반 접근 제어 시스템.",
              icon: <Shield className="w-10 h-10" />,
            },
          ].map((f, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: i * 0.2 }}
              viewport={{ once: true }}
            >
              <Card className="bg-card/70 backdrop-blur-xl border-border rounded-2xl shadow-lg">
                <CardContent className="p-8 text-center">
                  <div className="flex justify-center mb-6 text-primary">
                    {f.icon}
                  </div>
                  <h3 className="text-2xl font-semibold mb-3">{f.title}</h3>
                  <p className="text-muted-foreground">{f.desc}</p>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-28 px-6 text-center bg-card/50 backdrop-blur-lg border-t border-border">
        <h2 className="text-4xl font-bold">
          지금 바로 안전한 서비스를 시작하세요
        </h2>
        <p className="mt-4 max-w-xl mx-auto text-muted-foreground text-lg">
          이미 수많은 개발자들이 우리의 인증 시스템을 사용하고 있습니다.
        </p>

        <Button size="lg" className="mt-8 px-8 text-lg rounded-2xl">
          계정 생성하기
        </Button>
      </section>

      {/* Extra Section */}
      <section className="py-24 px-6">
        <h2 className="text-4xl font-bold text-center mb-12">
          왜 우리 인증 플랫폼인가요?
        </h2>

        <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10 text-muted-foreground">
          <div>
            <h3 className="text-2xl font-semibold mb-3 flex items-center gap-2">
              <Sparkles className="w-6 h-6 text-primary" /> AI 기반 보안
            </h3>
            <p>
              실시간 모니터링을 통해 의심스러운 활동을 감지하고 차단합니다.
            </p>
          </div>

          <div>
            <h3 className="text-2xl font-semibold mb-3 flex items-center gap-2">
              <Sparkles className="w-6 h-6 text-primary" /> 빠른 처리 속도
            </h3>
            <p>
              대규모 환경에서도 즉각적인 인증 처리를 제공합니다.
            </p>
          </div>

          <div>
            <h3 className="text-2xl font-semibold mb-3 flex items-center gap-2">
              <Sparkles className="w-6 h-6 text-primary" /> 개발자 친화적 API
            </h3>
            <p>
              간단하고 직관적인 API로 빠르게 연동할 수 있습니다.
            </p>
          </div>

          <div>
            <h3 className="text-2xl font-semibold mb-3 flex items-center gap-2">
              <Sparkles className="w-6 h-6 text-primary" /> 높은 커스터마이징
            </h3>
            <p>
              테마와 인증 흐름을 자유롭게 커스터마이징할 수 있습니다.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-10 text-center text-muted-foreground border-t border-border">
        © {new Date().getFullYear()} 미래형 인증 서비스. All rights reserved.
      </footer>
    </div>
  );
}