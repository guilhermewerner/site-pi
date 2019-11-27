using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using SitePi.Models;

namespace SitePi.Controllers
{
    public class UserController : Controller
    {
        private AppContext AppContext;

        public UserController(AppContext AppContext)
        {
            this.AppContext = AppContext;
        }
        
        [HttpGet("User/{id}")]
        public IActionResult Index(int Id)
        {
            User user = AppContext.Users.Find(Id);
            
            if (user != null)
            {
                ViewBag.User = user;

                ViewBag.Bots = AppContext.Bots.Where(b => b.UserId == user.Id);

                float TotalA = 0;
                float TotalB = 0;
                float TotalC = 0;
                float TotalD = 0;
                
                foreach (Bot b in ViewBag.Bots)
                {
                    TotalA = TotalA + (b.PercentageA * 7) / 100;
                    TotalB = TotalB + (b.PercentageB * 7) / 100;
                    TotalC = TotalC + (b.PercentageC * 7) / 100;
                    TotalD = TotalD + (b.PercentageD * 7) / 100;
                }

                ViewData["TotalA"] = TotalA;
                ViewData["TotalB"] = TotalB;
                ViewData["TotalC"] = TotalC;
                ViewData["TotalD"] = TotalD;

                return View();
            }

            return RedirectToAction("Index", "Home");
        }

        public IActionResult New()
        {
            return View("Form");
        }

        [HttpPost]
        public IActionResult New(User user)
        {
            if (ModelState.IsValid)
            {
                AppContext.Users.Add(user);
                AppContext.SaveChanges();

                return RedirectToAction("Index", "Home");
            }

            return View("Form", user);
        }
        
        public IActionResult Edit(int id)
        {
            User user = AppContext.Users.Find(id);

            if (user == null)
            {
                return RedirectToAction("Index");
            }

            return View("Form", user);
        }

        [HttpPost]
        public IActionResult Edit(User user)
        {
            if (ModelState.IsValid)
            {
                AppContext.Users.Update(user);
                AppContext.SaveChanges();

                return RedirectToAction("Index", "Home");
            }

            return View("Form", user);
        }

        public IActionResult Delete(int id)
        {
            User user = AppContext.Users.Find(id);

            if (user != null)
            {
                AppContext.Users.Remove(user);
                AppContext.SaveChanges();
            }

            return RedirectToAction("Index", "Home");
        }
    }
}